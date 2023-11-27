package cn.paper_card.smurf;

import cn.paper_card.database.DatabaseApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

class SmurfApiImpl implements SmurfApi {

    private final @NotNull DatabaseApi.MySqlConnection mySqlConnection;
    private Connection connection = null;
    private Table table = null;


    SmurfApiImpl(@NotNull DatabaseApi.MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
    }

    private @NotNull Table getTable() throws SQLException {
        final Connection newCon = this.mySqlConnection.getRowConnection();

        if (this.connection != null && this.connection == newCon) return this.table;

        this.connection = newCon;
        if (this.table != null) this.table.close();
        this.table = new Table(newCon);
        return this.table;
    }

    void close() throws SQLException {
        synchronized (this.mySqlConnection) {
            final Table t = this.table;
            if (t == null) {
                this.connection = null;
                return;
            }

            this.connection = null;
            this.table = null;

            t.close();
        }
    }


    @Override
    public void addSmurf(@NotNull SmurfInfo info) throws SQLException, IsAlreadySmurf, TheSmurfIsMain, TheSmurfIsSelf {
        // A -> A X
        // A -> B X
        if (info.mainUuid().equals(info.smurfUuid())) {
            throw new TheSmurfIsSelf();
        }

        synchronized (this.mySqlConnection) {
            try {
                final Table t = this.getTable();

                // A -> B
                // C -> B X
                // 查询这个小号是不是已经是别人的小号了
                {
                    final SmurfInfo i = t.queryBySmurfUuid(info.smurfUuid());
                    this.mySqlConnection.setLastUseTime();

                    if (i != null) {
                        throw new IsAlreadySmurf(i);
                    }
                }

                // A -> B
                // B -> C X
                // 查询这个大号是不是别人的小号
                {
                    final SmurfInfo i = t.queryBySmurfUuid(info.mainUuid());
                    this.mySqlConnection.setLastUseTime();

                    if (i != null) {
                        throw new IsAlreadySmurf(i);
                    }
                }


                // A -> B
                // C -> A X
                // 这个小号不能是一个大号
                {
                    final List<SmurfInfo> list = t.queryByMainUuidWithPage(info.smurfUuid(), 1, 0);
                    this.mySqlConnection.setLastUseTime();

                    if (!list.isEmpty()) {
                        throw new TheSmurfIsMain(list.get(0));
                    }
                }

                final int inserted = t.insert(info);
                this.mySqlConnection.setLastUseTime();

                if (inserted != 1) throw new RuntimeException("插入了%d条数据！".formatted(inserted));

            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public boolean removeSmurf(@NotNull UUID main, @NotNull UUID smurf) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final Table t = this.getTable();
                final int delete = t.delete(main, smurf);

                if (delete == 1) return true;
                if (delete == 0) return false;

                throw new RuntimeException("删除了%d条数据！".formatted(delete));

            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @Nullable SmurfInfo queryBySmurfUuid(@NotNull UUID uuid) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final Table t = this.getTable();

                final SmurfInfo info = t.queryBySmurfUuid(uuid);
                this.mySqlConnection.setLastUseTime();

                return info;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @NotNull List<SmurfInfo> queryByMainUuid(@NotNull UUID uuid, int limit, int offset) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final Table t = this.getTable();
                final List<SmurfInfo> list = t.queryByMainUuidWithPage(uuid, limit, offset);
                this.mySqlConnection.setLastUseTime();
                return list;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }
}
