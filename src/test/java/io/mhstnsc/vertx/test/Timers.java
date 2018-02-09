package io.mhstnsc.vertx.test;

import io.mhstnsc.vertx.test.testutils.TestBase;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import org.junit.Test;

import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;


public class Timers extends TestBase
{
    public Timers()
    {
        super(false);
    }

    @Test
    public void testCancelFiredTimer_WorkerContext() throws Exception
    {
        executeCancelFiredTimer(true);
    }

    @Test
    public void testCancelFiredTimer_EventloopContext() throws Exception
    {
        executeCancelFiredTimer(false);
    }

    public void executeCancelFiredTimer(boolean useWorkerContext) throws Exception
    {
        CompletableFuture<Void> timerFired = new CompletableFuture<>();

        vertx.deployVerticle(
                new AbstractVerticle()
                {
                    @Override
                    public void start() throws Exception
                    {
                        super.start();

                        // start a long timer
                        long cancellableTimerId = vertx.setTimer(100, event ->
                        {
                            timerFired.complete(null);
                            System.out.println("Timer fired. Bleah");
                        });

                        vertx.runOnContext(
                                event ->
                                {
                                    try
                                    {
                                        // sleep more than the cancellable timer (it should be fired now)
                                        Thread.sleep(200);
                                    }
                                    catch (InterruptedException e)
                                    {
                                        e.printStackTrace();
                                    }
                                    System.out.println("Cancelling timer");
                                    vertx.cancelTimer(cancellableTimerId);
                                }
                        );
                    }

                    @Override
                    public void stop() throws Exception
                    {
                        super.stop();
                    }
                },
                new DeploymentOptions().setWorker(useWorkerContext)
        );

        try
        {
            timerFired.get(1000, TimeUnit.MILLISECONDS);
            assertTrue(false);
        }
        catch(Exception e)
        {

        }
    }
}
