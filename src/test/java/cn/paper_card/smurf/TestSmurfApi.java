package cn.paper_card.smurf;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.UUID;

public class TestSmurfApi {

    private MyConnection connection;

    @Before
    public void before() {
        connection = new MyConnection();
    }

    @After
    public void after() throws SQLException {
        connection.close();
    }

    @Test
    public void test1() throws SQLException {
        final SmurfApiImpl api = new SmurfApiImpl(connection);

        api.close();
    }

    @Test
    public void test2() throws SQLException, SmurfApi.IsAlreadySmurf, SmurfApi.TheSmurfIsMain, SmurfApi.TheSmurfIsSelf {
        final SmurfApiImpl api = new SmurfApiImpl(connection);

        final SmurfApi.SmurfInfo info = new SmurfApi.SmurfInfo(UUID.randomUUID(), "Paper99",
                UUID.randomUUID(), "Paper100",
                System.currentTimeMillis(), "Test");

        // 删除
        {
            final SmurfApi.SmurfInfo i = api.queryBySmurfUuid(info.smurfUuid());
            if (i != null) {
                final boolean removed = api.removeSmurf(i.mainUuid(), i.smurfUuid());
                Assert.assertTrue(removed);
            }
        }

        { // 删除
            final SmurfApi.SmurfInfo i = api.queryBySmurfUuid(info.mainUuid());
            if (i != null) {
                api.removeSmurf(i.mainUuid(), i.smurfUuid());
                final boolean removed = api.removeSmurf(i.mainUuid(), i.smurfUuid());
                Assert.assertTrue(removed);
            }
        }

        // 添加
        api.addSmurf(info);

        // 查询1
        {
            final SmurfApi.SmurfInfo i = api.queryBySmurfUuid(info.mainUuid());
            Assert.assertNull(i);
        }

        // 查询2
        {
            final SmurfApi.SmurfInfo i = api.queryBySmurfUuid(info.smurfUuid());
            Assert.assertEquals(info, i);
        }


        // 删除
        final boolean removed = api.removeSmurf(info.mainUuid(), info.smurfUuid());
        Assert.assertTrue(removed);

        api.close();
    }

    @Test
    public void test3() throws SQLException {
        final SmurfApiImpl api = new SmurfApiImpl(connection);

        final SmurfApi.SmurfInfo info = new SmurfApi.SmurfInfo(UUID.randomUUID(), "Paper99",
                UUID.randomUUID(), "Paper100",
                System.currentTimeMillis(), "Test");

        try {
            api.addSmurf(info);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (SmurfApi.IsAlreadySmurf e) {
            System.out.println(e.getSmurfInfo());
            throw new RuntimeException(e);
        } catch (SmurfApi.TheSmurfIsMain e) {
            System.out.println(e.getSmurfInfo());
            throw new RuntimeException(e);
        } catch (SmurfApi.TheSmurfIsSelf e) {
            System.out.println(e.getClass().getSimpleName());
            throw new RuntimeException(e);
        }

        api.close();
    }
}
