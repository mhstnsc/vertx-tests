package io.vertx.test.testutils;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;


/**
 * Deploys event-loop verticles as specified in numberOfInstances
 */
public class MaxRateExecutor
{
    Logger logger = LoggerFactory.getLogger(MaxRateExecutor.class);

    private AtomicLong intervalRequestCounter = new AtomicLong();

    static public MaxRateExecutor create()
    {
        return new MaxRateExecutor();
    }

    public interface TestedCode
    {
        void init(Future<Void> future);
        void run(Runnable finished);
    }

    public enum ClientType
    {
        Async,
        Blocking
    }

    public void start(
            Vertx vertx,
            int numberOfInstances,
            Supplier<TestedCode> testedCodeSupplier
    ) throws Exception
    {
        vertx.setPeriodic(
                1000,
                event ->
                {
                    AtomicLong currentRequestCounter = intervalRequestCounter;
                    intervalRequestCounter = new AtomicLong();

                    logger.info("Rate: {} req/sec", currentRequestCounter.get());
                }
        );

        for(int i=0; i<numberOfInstances; i++)
        {
            TestedCode testedCode = testedCodeSupplier.get();

            String id = AwaitUtils.awaitResult(
                    asyncResultHandler ->
                            vertx.deployVerticle(
                                    new AbstractVerticle()
                                    {
                                        @Override
                                        public void start(Future<Void> startFuture) throws Exception
                                        {
                                            testedCode.init(
                                                    Future.<Void>future().setHandler(
                                                            event ->
                                                            {
                                                                doRequest();
                                                                startFuture.complete();
                                                            }
                                                    )
                                            );
                                        }

                                        private void doRequest()
                                        {
                                            testedCode.run(
                                                    () ->
                                                    {
                                                        intervalRequestCounter.incrementAndGet();
                                                        doRequest();
                                                    }
                                            );
                                        }
                                    },
                                    new DeploymentOptions(),
                                    asyncResultHandler
                            )
            );
        }
    }
}
