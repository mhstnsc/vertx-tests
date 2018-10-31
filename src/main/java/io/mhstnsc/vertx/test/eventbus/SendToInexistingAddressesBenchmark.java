package io.mhstnsc.vertx.test.eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static io.mhstnsc.vertx.test.utils.AwaitUtils.awaitResult;


/**
 * Test creates a number of event verticles
 * Each verticle performs the following:
 *  - send a message to inexisting address
 *  - when handler is invoked then repeat
  *
 *  To run:
 *  gradle shadowJar
 *  java -cp ./build/libs/vertx-tests-0.0.1-SNAPSHOT-fat.jar io.mhstnsc.vertx.test.SendToInexistingAddressesTest
 */
public class SendToInexistingAddressesBenchmark
{
    static Vertx vertx;
    static final int numberOfVerticles = 200;  // number of verticles which create load

    static AtomicLong counter = new AtomicLong();

    public static void main(String[] args) throws Exception
    {
        vertx = awaitResult(
                h -> Vertx.clusteredVertx(
                        new VertxOptions().setClusterManager(new HazelcastClusterManager()),
                        h
                )
        );

        setupPrintRate();  // start periodically printing the rate

        for (int i = 0; i < numberOfVerticles; i++)
        {
            vertx.deployVerticle(
                    new AbstractVerticle()
                    {
                        @Override
                        public void start() throws Exception
                        {
                            runTestStep();
                        }

                        void runTestStep()
                        {
                            String address = UUID.randomUUID().toString();

                            vertx.eventBus()
                                 .send(
                                         address,
                                         "bla",
                                         event ->
                                         {
                                             counter.incrementAndGet();
                                             runTestStep();
                                         }
                                 );
                        }
                    }
            );
        }

    }

    private static void setupPrintRate()
    {
        // setup periodic rate display
        vertx.setPeriodic(
                5000,
                event ->
                {
                    vertx.executeBlocking(
                            event1 ->
                            {
                                long counterValue = counter.getAndSet(0);

                                System.out.println(String.format("send-rate: %d req/s",
                                        counterValue / 5
                                ));

                                event1.complete();
                            },
                            event1 -> {}
                    );
                }
        );

    }


}

