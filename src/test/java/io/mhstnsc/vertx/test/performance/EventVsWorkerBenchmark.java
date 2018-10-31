package io.mhstnsc.vertx.test.performance;

import io.mhstnsc.vertx.test.utils.MaxRateExecutor;
import io.mhstnsc.vertx.test.core.TestBase;
import io.vertx.core.Future;
import io.vertx.core.VertxOptions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class EventVsWorkerBenchmark extends TestBase
{
    private static final Logger logger = Logger.getLogger(EventVsWorkerBenchmark.class.getName());
    static final int TOTAL_VERTICLES = 500;
    static final int TOTAL_AMOUNT_OF_KEYS = 1000000;
    static final int TOTAL_AMOUNT_OF_KEYS_PER_VERTICLE = TOTAL_AMOUNT_OF_KEYS / TOTAL_VERTICLES;
    static final int PRELOAD_KEYS_PERCENT = 90;
    static final int AMOUNT_PRELOAD_KEYS = TOTAL_AMOUNT_OF_KEYS_PER_VERTICLE * PRELOAD_KEYS_PERCENT / 100;
    static final int BATCH_SIZE = 1;

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
    public void testEventLoopBasedOperations() throws Exception
    {
        executeTest(MaxRateExecutor.DeployType.Async);
    }

    @Test
    public void testWorkerBasedOperations() throws Exception
    {
        executeTest(MaxRateExecutor.DeployType.Blocking);
    }

    private void executeTest(MaxRateExecutor.DeployType deployType) throws Exception
    {
        MaxRateExecutor
                .create()
                .start(
                        vertx,
                        30,
                        deployType,
                        () -> new MaxRateExecutor.TestedCode()
                        {
                            Set<Integer> set = new HashSet<>();
                            List<Integer> list = new ArrayList<>(TOTAL_AMOUNT_OF_KEYS_PER_VERTICLE);
                            int addIdx = 0;
                            int removeIdx = 0;

                            @Override
                            public void init(Future<Void> future)
                            {
                                // generate a set of numbers shuffle and preload the set
                                list = IntStream
                                        .generate(() -> ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE))
                                        .limit(TOTAL_AMOUNT_OF_KEYS_PER_VERTICLE)
                                        .boxed()
                                        .collect(Collectors.toList());

                                while(addIdx < AMOUNT_PRELOAD_KEYS)
                                {
                                    set.add(list.get(addIdx));
                                    addIdx++;
                                }
                                future.complete();
                            }

                            @Override
                            public void run(Runnable finished)
                            {
                                for(int i=0; i<BATCH_SIZE; i++)
                                {
                                    set.remove(list.get(removeIdx));
                                    removeIdx = (removeIdx + 1) % list.size();
                                    set.add(list.get(addIdx));
                                    addIdx = (addIdx + 1) % list.size();
                                }

                                finished.run();
                            }
                        }
                );
        Thread.sleep(100000);
    }
}
