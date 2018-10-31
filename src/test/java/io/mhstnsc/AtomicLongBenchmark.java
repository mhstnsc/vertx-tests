package io.mhstnsc;

import io.mhstnsc.vertx.test.utils.MaxRateExecutor;
import io.mhstnsc.vertx.test.core.TestBase;
import io.vertx.core.Future;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;


public class AtomicLongBenchmark extends TestBase
{
    public AtomicLongBenchmark()
    {
        super(true);
    }

    @Test
    public void testConcurrentMaxUpdate() throws Exception
    {
        AtomicLong max = new AtomicLong();

        MaxRateExecutor.create()
                       .start(
                               vertx,
                               8,
                               MaxRateExecutor.DeployType.Async,
                               () -> new MaxRateExecutor.TestedCode()
                               {
                                   private ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();

                                   @Override
                                   public void init(Future<Void> future)
                                   {
                                       future.complete();
                                   }

                                   @Override
                                   public void run(Runnable finished)
                                   {
                                       long end = System.currentTimeMillis();

                                       long newVal = end - threadLocalRandom.nextLong(end);

                                       max.updateAndGet(operand -> operand > newVal? operand:newVal);
                                       finished.run();
                                   }
                               }
                       );

        Thread.sleep(100000);
    }
}
