package io.mhstnsc.vertx.test.ping;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static io.mhstnsc.vertx.test.AwaitUtils.awaitResult;


public class Receiver
{
    static Vertx vertx;
    static final int numberOfVerticles = 10;  // number of verticles which create load

    static AtomicLong counter = new AtomicLong();

    public static void main(String[] args) throws Exception
    {
        vertx = awaitResult(
                h -> Vertx.clusteredVertx(
                        new VertxOptions().setClusterManager(new HazelcastClusterManager()),
                        h
                )
        );

        for (int i = 0; i < numberOfVerticles; i++)
        {
            vertx.deployVerticle(
                    new AbstractVerticle()
                    {
                        Random random = new Random();

                        @Override
                        public void start() throws Exception
                        {
                            runTestStep();
                        }

                        void runTestStep()
                        {
                            String consumerAddress = "receiver" + counter.getAndIncrement();

                            System.out.println("Register receiver for " + consumerAddress);

                            vertx.eventBus()
                                 .consumer(
                                         consumerAddress,
                                         event ->
                                         {
                                             event.reply(null);
                                         }
                                 );
                        }
                    }
            );
        }
    }
}