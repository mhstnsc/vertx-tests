package io.mhstnsc.vertx.test.eventbus.ping;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;


public class Sender
{
    static Vertx vertx;
    static final int numberOfVerticles = 200;  // number of verticles which create load

    static AtomicLong counter = new AtomicLong();

    public static void main(String[] args) throws Exception
    {
//        vertx = awaitResult(
//                h -> Vertx.clusteredVertx(
//                        new VertxOptions().setClusterManager(new HazelcastClusterManager()),
//                        h
//                )
//        );

        vertx = Vertx.vertx();

        setupPrintRate();  // start periodically printing the rate


        for (int i = 0; i < 10; i++)
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
                            vertx.eventBus()
                                 .send(
                                         "receiver" + random.nextInt(10),
                                         new byte[256],
                                         event ->
                                         {
                                             if(event.failed())
                                             {
                                                 System.out.println("send failure " + event.cause().getMessage());
                                             }
                                             counter.incrementAndGet();

                                             vertx.setTimer(
                                                     100,
                                                     event1 -> runTestStep()
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