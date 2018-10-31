package io.mhstnsc.vertx.test.performance;

import io.mhstnsc.vertx.test.utils.MaxRateExecutor;
import io.mhstnsc.vertx.test.core.TestBase;
import io.mhstnsc.vertx.test.utils.PojoCodec;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


/** goal of the test is to see how concurrent hash map ranks against message passing */
public class ConcurrentVsParallelBenchmark extends TestBase
{
    private static final Logger logger = Logger.getLogger(EventVsWorkerBenchmark.class.getName());

    static final Map<Integer, String> concurentMap = new ConcurrentHashMap();
    static final int READ_PERCENT_FROM_TOTAL = 80;
    static final int TOTAL_AMOUNT_OF_KEYS = 1000000;

    static final int NUMBER_OF_SHARDS = 16;
    static final int TOTAL_AMOUNT_OF_KEYS_PER_SHARD = TOTAL_AMOUNT_OF_KEYS / NUMBER_OF_SHARDS;

    class PutRequest
    {
        public Integer integer;
        public String string;

        public PutRequest(int key)
        {
            integer = key;
            string = Integer.toString(integer);
        }
    }

    static String addressGet(Integer key)
    {
        return Integer.toString(key % 16);
    }

    static String addressPut(Integer key)
    {
        return Integer.toString(key % NUMBER_OF_SHARDS) + "Put";
    }

    @Override
    protected VertxOptions buildVertxOptions(VertxOptions options)
    {
        logger.info("Setting extra properties");
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        return options.setEventLoopPoolSize(16)
                      .setWorkerPoolSize(500);
    }

    @Test
    public void testConcurrent() throws Exception
    {
        DeliveryOptions pojoDeliveryOptions = new DeliveryOptions().setCodecName("pojo");

        vertx.eventBus().registerCodec(new PojoCodec());

        for(int i=0; i<NUMBER_OF_SHARDS; i++)
        {
            final int verticleId = i;
            vertx.deployVerticle(
                    new AbstractVerticle()
                    {
                        private final Map<Integer, String> localShardMap = new HashMap<>();

                        @Override
                        public void start(Future<Void> startFuture) throws Exception
                        {
                            for(int i = verticleId; i < TOTAL_AMOUNT_OF_KEYS ; i += NUMBER_OF_SHARDS)
                            {
                                localShardMap.put(i, "");
                            }

                            vertx.eventBus().<Integer>localConsumer(
                                    addressGet(verticleId),
                                    event ->
                                    {
                                        event.reply(localShardMap.get(event.body()), pojoDeliveryOptions);
                                    }
                            );

                            vertx.eventBus().<PutRequest>localConsumer(
                                    addressPut(verticleId),
                                    event ->
                                    {
                                        localShardMap.put(event.body().integer, event.body().string);
                                        event.reply(null, pojoDeliveryOptions);
                                    }
                            );
                        }
                    }
            );
        }


        // now the consumers
        MaxRateExecutor
                .create()
                .start(
                        vertx,
                        500,
                        MaxRateExecutor.DeployType.Blocking,
                        () -> new MaxRateExecutor.TestedCode()
                        {
                            Random opRandom = new Random();
                            Random keyRandom = new Random();

                            @Override
                            public void init(Future<Void> future)
                            {
                                future.complete();
                            }

                            @Override
                            public void run(Runnable finished)
                            {
                                // pick up some operation to do
                                boolean isRead = opRandom.nextInt(100) < READ_PERCENT_FROM_TOTAL;

                                int key = keyRandom.nextInt(TOTAL_AMOUNT_OF_KEYS);

                                if(isRead)
                                {
                                    vertx.eventBus().send(
                                            addressGet(key),
                                            key,
                                            pojoDeliveryOptions,
                                            event->
                                            {
                                                finished.run();
                                            }
                                    );
                                }
                                else
                                {
                                    vertx.eventBus().send(
                                            addressPut(key),
                                            new PutRequest(key),
                                            pojoDeliveryOptions,
                                            event ->
                                            {
                                                finished.run();
                                            }
                                    );
                                }
                            }
                        }
                );
        Thread.sleep(100000);
    }

    @Test
    public void testParallel() throws Exception
    {
        for(int i = 0; i < TOTAL_AMOUNT_OF_KEYS ; i++)
        {
            concurentMap.put(i, "");
        }

        // now the consumers
        MaxRateExecutor
                .create()
                .start(
                        vertx,
                        500,
                        MaxRateExecutor.DeployType.Blocking,
                        () -> new MaxRateExecutor.TestedCode()
                        {
                            Random opRandom = new Random();
                            Random keyRandom = new Random();

                            @Override
                            public void init(Future<Void> future)
                            {
                                future.complete();
                            }

                            @Override
                            public void run(Runnable finished)
                            {
                                // pick up some operation to do
                                boolean isRead = opRandom.nextInt(100) < READ_PERCENT_FROM_TOTAL;

                                int key = keyRandom.nextInt(TOTAL_AMOUNT_OF_KEYS);

                                if(isRead)
                                {
                                    concurentMap.get(key);
                                }
                                else
                                {
                                    concurentMap.put(key, new PutRequest(key).string);
                                }
                                finished.run();
                            }
                        }
                );
        Thread.sleep(100000);
    }
}
