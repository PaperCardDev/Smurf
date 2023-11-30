package cn.paper_card.smurf;

import cn.paper_card.smurf.api.SmurfInfo;
import cn.paper_card.smurf.api.exception.AlreadyIsSmurfException;
import cn.paper_card.smurf.api.exception.SmurfIsMainException;
import cn.paper_card.smurf.api.exception.SmurfIsSelfException;
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
        final SmurfServiceImpl api = new SmurfServiceImpl(connection);

        api.close();
    }

    @Test
    public void test2() throws SQLException, SmurfIsMainException, SmurfIsSelfException, AlreadyIsSmurfException {
        final SmurfServiceImpl api = new SmurfServiceImpl(connection);

        final SmurfInfo info = new SmurfInfo(UUID.randomUUID(), "Paper99",
                UUID.randomUUID(), "Paper100",
                System.currentTimeMillis(), "Test");

        // 删除
        {
            final SmurfInfo i = api.queryBySmurfUuid(info.smurfUuid());
            if (i != null) {
                final boolean removed = api.removeSmurf(i.mainUuid(), i.smurfUuid());
                Assert.assertTrue(removed);
            }
        }

        { // 删除
            final SmurfInfo i = api.queryBySmurfUuid(info.mainUuid());
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
            final SmurfInfo i = api.queryBySmurfUuid(info.mainUuid());
            Assert.assertNull(i);
        }

        // 查询2
        {
            final SmurfInfo i = api.queryBySmurfUuid(info.smurfUuid());
            Assert.assertEquals(info, i);
        }


        // 删除
        final boolean removed = api.removeSmurf(info.mainUuid(), info.smurfUuid());
        Assert.assertTrue(removed);

        api.close();
    }

    @Test
    public void test3() throws SQLException, SmurfIsMainException, SmurfIsSelfException, AlreadyIsSmurfException {
        final SmurfServiceImpl api = new SmurfServiceImpl(connection);

        final SmurfInfo info = new SmurfInfo(UUID.randomUUID(), "Paper99",
                UUID.randomUUID(), "Paper100",
                System.currentTimeMillis(), "Test");

        api.addSmurf(info);


        api.close();
    }
}
