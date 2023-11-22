package cn.paper_card.xiaohao;


import cn.paper_card.database.DatabaseConnection;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class Table {
    private final static String NAME = "xiao_hao";

    private final @NotNull Connection connection;

    private PreparedStatement statementInsert = null;

    private PreparedStatement statementQueryDaByXiao = null;

    private PreparedStatement statementQueryCountByDa = null;

    private PreparedStatement statementDeleteByXiao = null;

    Table(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.create();
    }

    private void create() throws SQLException {
        DatabaseConnection.createTable(this.connection, """
                CREATE TABLE IF NOT EXISTS %s (
                    uid1 BIGINT NOT NULL,
                    uid2 BIGINT NOT NULL,
                    uid3 BIGINT NOT NULL,
                    uid4 BIGINT NOT NULL,
                    PRIMARY KEY(uid1, uid2, uid3, uid4)
                )""".formatted(NAME));
    }

    void close() throws SQLException {
        DatabaseConnection.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement
                    ("INSERT INTO %s (uid1, uid2, uid3, uid4) VALUES (?, ?, ?, ?)".formatted(NAME));
        }
        return this.statementInsert;
    }

    private @NotNull PreparedStatement getStatementQueryDaByXiao() throws SQLException {
        if (this.statementQueryDaByXiao == null) {
            this.statementQueryDaByXiao = this.connection.prepareStatement
                    ("SELECT uid1, uid2 FROM %s WHERE uid3=? AND uid4=? LIMIT 1 OFFSET 0".formatted(NAME));
        }
        return this.statementQueryDaByXiao;
    }

    private @NotNull PreparedStatement getStatementQueryCountByDa() throws SQLException {
        if (this.statementQueryCountByDa == null) {
            this.statementQueryCountByDa = this.connection.prepareStatement
                    ("SELECT count(*) FROM %s WHERE uid1=? AND uid2=?".formatted(NAME));
        }
        return this.statementQueryCountByDa;
    }

    private @NotNull PreparedStatement getStatementDeleteByXiao() throws SQLException {
        if (this.statementDeleteByXiao == null) {
            this.statementDeleteByXiao = this.connection.prepareStatement
                    ("DELETE FROM %s WHERE uid3=? AND uid4=?".formatted(NAME));
        }
        return this.statementDeleteByXiao;
    }

    int insert(@NotNull UUID dahao, @NotNull UUID xiaohao) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();
        ps.setLong(1, dahao.getMostSignificantBits());
        ps.setLong(2, dahao.getLeastSignificantBits());
        ps.setLong(3, xiaohao.getMostSignificantBits());
        ps.setLong(4, xiaohao.getLeastSignificantBits());
        return ps.executeUpdate();
    }

    private @NotNull List<UUID> parseUuids(@NotNull ResultSet resultSet) throws SQLException {

        final List<UUID> list = new LinkedList<>();

        try {
            while (resultSet.next()) {
                final long uid1 = resultSet.getLong(1);
                final long uid2 = resultSet.getLong(2);
                final UUID uuid = new UUID(uid1, uid2);
                list.add(uuid);
            }
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        resultSet.close();
        return list;
    }

    @NotNull List<UUID> queryDaByXiao(@NotNull UUID xiaohao) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryDaByXiao();
        ps.setLong(1, xiaohao.getMostSignificantBits());
        ps.setLong(2, xiaohao.getLeastSignificantBits());

        final ResultSet resultSet = ps.executeQuery();

        return this.parseUuids(resultSet);
    }

    int queryCountByDa(@NotNull UUID dahao) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryCountByDa();
        ps.setLong(1, dahao.getMostSignificantBits());
        ps.setLong(2, dahao.getLeastSignificantBits());

        final ResultSet resultSet = ps.executeQuery();

        final int c;

        try {
            if (resultSet.next()) {
                c = resultSet.getInt(1);
            } else throw new SQLException("不应该没有数据！");

            if (resultSet.next()) throw new SQLException("不应该还有数据！");
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        resultSet.close();
        return c;
    }

    int deleteByXiao(@NotNull UUID xiao) throws SQLException {
        final PreparedStatement ps = this.getStatementDeleteByXiao();
        ps.setLong(1, xiao.getMostSignificantBits());
        ps.setLong(2, xiao.getLeastSignificantBits());
        return ps.executeUpdate();
    }
}
