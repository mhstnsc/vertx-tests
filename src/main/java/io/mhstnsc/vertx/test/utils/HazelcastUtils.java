package io.mhstnsc.vertx.test.utils;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxImpl;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.Set;


public class HazelcastUtils
{

    public static HazelcastInstance getHazelcastInstance(Vertx vertx)
    {
        VertxImpl vimpl = (VertxImpl) vertx;
        HazelcastClusterManager clusterManager = (HazelcastClusterManager) vimpl.getClusterManager();
        return clusterManager.getHazelcastInstance();
    }

}
