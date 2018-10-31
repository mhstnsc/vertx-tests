package io.mhstnsc.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import io.mhstnsc.vertx.test.utils.HazelcastUtils;
import io.mhstnsc.vertx.test.core.TestBase;
import org.junit.Test;

import java.util.UUID;


public class LocksTest extends TestBase
{
    /* checks if map locks are GC-ed after unlocking */
    @Test
    public void testManyLocksGC() throws Exception
    {
        HazelcastInstance hazelcastInstance = HazelcastUtils.getHazelcastInstance(vertx);

        IMap<String,String> bla = hazelcastInstance.getMap("testManyLocksGC");

        Thread.sleep(30000);
        System.out.println("Start creating locks");

        for(int i=0; i<100000;i++)
        {
            String lockName = UUID.randomUUID().toString();

            ILock lock = hazelcastInstance.getLock(lockName);
            lock.lock();
            lock.forceUnlock();

//            bla.lock(lockName);
//            bla.forceUnlock(lockName);
        }

        System.out.println("Finished creating locks");

        Thread.sleep(1000000);
    }

}
