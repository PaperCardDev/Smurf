package cn.paper_card.smurf;

import cn.paper_card.database.api.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class Table {
    private final static String NAME = "smurf";

    private final static String COLS = "m_uid1, m_uid2, m_name, s_uid1, s_uid2, s_name, c_time, remark";

    private final @NotNull Connection connection;

    private PreparedStatement statementInsert = null;

    private PreparedStatement statementQueryBySmurfUuid = null;

    private PreparedStatement statementQueryByMainUuidWithPage = null;

    private PreparedStatement statementDelete = null;

    Table(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.create();
    }

    private void create() throws SQLException {
        Util.executeSQL(this.connection, """
                CREATE TABLE IF NOT EXISTS %s (
                    m_uid1 BIGINT NOT NULL,
                    m_uid2 BIGINT NOT NULL,
                    m_name VARCHAR(64) NOT NULL,
                    s_uid1 BIGINT NOT NULL,
                    s_uid2 BIGINT NOT NULL,
                    s_name VARCHAR(64) NOT NULL,
                    c_time BIGINT NOT NULL,
                    remark VARCHAR(128) NOT NULL,
                    PRIMARY KEY(m_uid1, m_uid2, s_uid1, s_uid2)
                )""".formatted(NAME));
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getStatementInsert() throws SQLException {
        if (this.statementInsert == null) {
            this.statementInsert = this.connection.prepareStatement("""
                    INSERT INTO %s (m_uid1, m_uid2, m_name, s_uid1, s_uid2, s_name, c_time, remark)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)""".formatted(NAME));
        }
        return this.statementInsert;
    }

    private @NotNull PreparedStatement getStatementQueryBySmurfUuid() throws SQLException {
        if (this.statementQueryBySmurfUuid == null) {
            this.statementQueryBySmurfUuid = this.connection.prepareStatement
                    ("SELECT %s FROM %s WHERE s_uid1=? AND s_uid2=? LIMIT 1".formatted(COLS, NAME));
        }
        return this.statementQueryBySmurfUuid;
    }

    private @NotNull PreparedStatement getStatementQueryByMainUuidWithPage() throws SQLException {
        if (this.statementQueryByMainUuidWithPage == null) {
            this.statementQueryByMainUuidWithPage = this.connection.prepareStatement("""
                    SELECT %s FROM %s WHERE m_uid1=? AND m_uid2=? LIMIT ? OFFSET ?"""
                    .formatted(COLS, NAME));
        }
        return this.statementQueryByMainUuidWithPage;
    }

    private @NotNull PreparedStatement getStatementDelete() throws SQLException {
        if (this.statementDelete == null) {
            this.statementDelete = this.connection.prepareStatement
                    ("DELETE FROM %s WHERE m_uid1=? AND m_uid2=? AND s_uid1=? AND s_uid2=? LIMIT 1".formatted(NAME));
        }
        return this.statementDelete;
    }

    private @NotNull SmurfApi.SmurfInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        final long mUid1 = resultSet.getLong(1);
        final long mUid2 = resultSet.getLong(2);
        final String mName = resultSet.getString(3);

        final long sUid1 = resultSet.getLong(4);
        final long sUid2 = resultSet.getLong(5);
        final String sName = resultSet.getString(6);

        final long cTime = resultSet.getLong(7);
        final String remark = resultSet.getString(8);

        return new SmurfApi.SmurfInfo(new UUID(mUid1, mUid2), mName,
                new UUID(sUid1, sUid2), sName,
                cTime, remark);
    }

    private @Nullable SmurfApi.SmurfInfo parseOne(@NotNull ResultSet resultSet) throws SQLException {

        final SmurfApi.SmurfInfo info;

        try {
            if (resultSet.next()) info = this.parseRow(resultSet);
            else info = null;

            if (resultSet.next()) throw new SQLException("不应该还有数据！");
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        resultSet.close();

        return info;
    }

    private @NotNull List<SmurfApi.SmurfInfo> parseAll(@NotNull ResultSet resultSet) throws SQLException {
        final List<SmurfApi.SmurfInfo> list = new LinkedList<>();

        try {
            while (resultSet.next()) {
                final SmurfApi.SmurfInfo info = this.parseRow(resultSet);
                list.add(info);
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

    int insert(@NotNull SmurfApi.SmurfInfo info) throws SQLException {
        final PreparedStatement ps = this.getStatementInsert();

        ps.setLong(1, info.mainUuid().getMostSignificantBits());
        ps.setLong(2, info.mainUuid().getLeastSignificantBits());
        ps.setString(3, info.mainName());

        ps.setLong(4, info.smurfUuid().getMostSignificantBits());
        ps.setLong(5, info.smurfUuid().getLeastSignificantBits());
        ps.setString(6, info.smurfName());

        ps.setLong(7, info.time());
        ps.setString(8, info.remark());

        return ps.executeUpdate();
    }

    @Nullable SmurfApi.SmurfInfo queryBySmurfUuid(@NotNull UUID uuid) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryBySmurfUuid();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        final ResultSet resultSet = ps.executeQuery();

        return this.parseOne(resultSet);
    }

    @NotNull List<SmurfApi.SmurfInfo> queryByMainUuidWithPage(@NotNull UUID uuid, int limit, int offset) throws SQLException {
        final PreparedStatement ps = this.getStatementQueryByMainUuidWithPage();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        ps.setInt(3, limit);
        ps.setInt(4, offset);
        final ResultSet resultSet = ps.executeQuery();
        return this.parseAll(resultSet);
    }

    int delete(@NotNull UUID main, @NotNull UUID smurf) throws SQLException {
        final PreparedStatement ps = this.getStatementDelete();
        ps.setLong(1, main.getMostSignificantBits());
        ps.setLong(2, main.getLeastSignificantBits());
        ps.setLong(3, smurf.getMostSignificantBits());
        ps.setLong(4, smurf.getLeastSignificantBits());
        return ps.executeUpdate();
    }
}
