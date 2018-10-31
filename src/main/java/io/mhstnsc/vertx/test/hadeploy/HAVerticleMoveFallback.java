package io.mhstnsc.vertx.test.hadeploy;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.concurrent.atomic.AtomicLong;

import static io.mhstnsc.vertx.test.utils.AwaitUtils.awaitResult;


public class HAVerticleMoveFallback
{
    static Vertx vertx;
    static final int numberOfVerticles = 200;  // number of verticles which create load

    static AtomicLong counter = new AtomicLong();

    public static void main(String[] args) throws Exception
    {
        vertx = awaitResult(
                h -> Vertx.clusteredVertx(
                        new VertxOptions()
                                .setClusterManager(new HazelcastClusterManager())
                                .setHAEnabled(true)
                                .setHAGroup("bla"),
                        h
                )
        );

//        vertx = Vertx.vertx();
    }
}
