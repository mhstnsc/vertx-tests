package io.mhstnsc.vertx.test.eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static io.mhstnsc.vertx.test.utils.AwaitUtils.awaitResult;


/**
 * Test creates a number of event verticles
 * Each verticle performs the following:
 *  - register a handler for an address
 *  - when registration finished then unregister the address
 *  - when unregistration finished then repeat
 *
 *  To run:
 *  gradle shadowJar
 *  java -cp ./build/libs/vertx-tests-0.0.1-SNAPSHOT-fat.jar io.mhstnsc.vertx.test.RegisterUnregisterThroughputTest
 */
public class RegisterUnregisterBenchmark
{
    static Vertx vertx;
    static final int numberOfVerticles = 200;  // number of verticles which create load

    static AtomicLong registerCounter = new AtomicLong();
    static AtomicLong unregisterCounter = new AtomicLong();

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

                            // register a consumer for the random address which is then later unregistered

                            MessageConsumer<?> consumer = vertx.eventBus()
                                                               .consumer(
                                                                       address,
                                                                       event ->
                                                                       {
                                                                       }
                                                               );
                            consumer.completionHandler(
                                    event ->
                                    {
                                        // once the consumer was registered then unregister it
                                        registerCounter.incrementAndGet();


                                        consumer.unregister(
                                                event1 ->
                                                {
                                                    // once the consumer was unregistered then repeat
                                                    unregisterCounter.incrementAndGet();

                                                    runTestStep();
                                                }
                                        );
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
                                long registerCounterValue = registerCounter.getAndSet(0);
                                long unregisterCounterValue = unregisterCounter.getAndSet(0);

                                System.out.println(String.format("register-rate: %d req/s, unregister-rate: %d req/s",
                                        registerCounterValue / 5,
                                        unregisterCounterValue / 5
                                ));

                                event1.complete();
                            },
                            event1 -> {}
                    );
                }
        );

    }


}
