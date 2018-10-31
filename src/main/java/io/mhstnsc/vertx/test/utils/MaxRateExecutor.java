package io.mhstnsc.vertx.test.utils;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Snapshot;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static io.mhstnsc.vertx.test.utils.AwaitUtils.awaitResult;


/**
 * Deploys event-loop verticles as specified in numberOfInstances
 *
 * The class will never stop
 */
public class MaxRateExecutor
{
    private static final Logger logger = LoggerFactory.getLogger(MaxRateExecutor.class);

    private AtomicLong intervalRequestCounter = new AtomicLong();
    private Queue<AtomicLong> historyCounters = new LinkedList<>();

    static public MaxRateExecutor create()
    {
        return new MaxRateExecutor();
    }

    public interface TestedCode
    {
        /**
         * Called one at the beginning
         */
        void init(Future<Void> future);

        /**
         * Called in a loop to perform a computation
         */
        void run(Runnable finished);
    }

    public enum DeployType
    {
        Async,
        Blocking
    }

    private void startPeriodicRateComputation(
            List<Histogram> testedCodeInstances
    )
    {
        Vertx.vertx().setPeriodic(
                1000,
                event ->
                {
                    AtomicLong currentRequestCounter = intervalRequestCounter;
                    intervalRequestCounter = new AtomicLong();

                    if(historyCounters.size() == 15)
                    {
                        historyCounters.remove();
                    }
                    historyCounters.add(currentRequestCounter);

                    long counter = 0;
                    for (AtomicLong historyCounter : historyCounters)
                    {
                        counter += historyCounter.get();
                    }

                    Snapshot runTimeSnapshot = testedCodeInstances.get(0).getSnapshot();

                    logger.info("Rate: {} req/sec 15secAvg: {} req/sec, 95thReqTime: {}, AvgReqTime: {}",
                            formatRate(currentRequestCounter.get()),
                            formatRate(counter / 15),
                            formatNanoDuration(runTimeSnapshot.get95thPercentile()),
                            formatNanoDuration(runTimeSnapshot.getMean())
                    );
                }
        );
    }

    private String formatRate(long rate)
    {
        if (rate >= 1000000 && rate <= 1000000000)
        {
            return String.format("%dM", (int)(rate / 1000000));
        }
        if (rate >= 1000 && rate < (100000-1))
        {
            return String.format("%dK", (int)(rate / 1000));
        }
        return String.format("%d", (int)(rate));
    }

    private String formatNanoDuration(double duration)
    {
        if (duration >= 1000000)
        {
            return String.format("%4d ms", (int)(duration / 1000000));
        }
        if (duration >= 1000)
        {
            return String.format("%3d us", (int)(duration / 1000));
        }
        return String.format("%3d ns", (int)(duration));
    }

    public MaxRateExecutor startAsync(
            Vertx vertx,
            Supplier<TestedCode> testedCodeSupplier
    ) throws Exception
    {
        return start(
                vertx,
                Runtime.getRuntime().availableProcessors(),
                DeployType.Async,
                testedCodeSupplier
        );
    }

    public MaxRateExecutor start(
            Vertx vertx,
            int numberOfInstances,
            DeployType deployType,
            Supplier<TestedCode> testedCodeSupplier
    ) throws Exception
    {
        List<Histogram> histograms = new ArrayList<>();

        AtomicInteger initBarrier = new AtomicInteger();
        initBarrier.set(numberOfInstances);

        for(int i=0; i<numberOfInstances; i++)
        {
            TestedCode testedCode = testedCodeSupplier.get();

            Histogram runTimeHisto = new Histogram(new SlidingWindowReservoir(100));
            histograms.add(runTimeHisto);

            @SuppressWarnings("unused")
            String id = awaitResult(
                    asyncResultHandler ->
                            vertx.deployVerticle(
                                    new AbstractVerticle()
                                    {
                                        boolean executing;

                                        @Override
                                        public void start(Future<Void> startFuture) throws Exception
                                        {
                                            try
                                            {
                                                testedCode.init(
                                                        Future.<Void>future().setHandler(
                                                                event2 ->
                                                                {
                                                                    vertx.eventBus().consumer(
                                                                            "maxrate.start",
                                                                            event -> doRequest()
                                                                    ).completionHandler(
                                                                            startFuture.completer()
                                                                    );
                                                                }
                                                        )
                                                );
                                            }
                                            catch(Throwable e)
                                            {
                                                logger.error("Uncaught: ", e);
                                            }

                                        }

                                        private void doRequest()
                                        {
                                            executing = true;

                                            long startTimeNs = System.nanoTime();

                                            testedCode.run(
                                                    () ->
                                                    {
                                                        long diff = System.nanoTime() - startTimeNs;
                                                        if(diff > 0)
                                                        {
                                                            runTimeHisto.update(diff);
                                                        }
                                                        intervalRequestCounter.incrementAndGet();

                                                        Vertx.currentContext().runOnContext(
                                                                event ->
                                                                {
                                                                    doRequest();
                                                                }
                                                        );
//                                                        if(executing)
//                                                        {
//                                                            // need to do this to be reentrant (just in case)
//
//                                                        }
//                                                        else
//                                                        {
//                                                            doRequest();
//                                                        }
                                                    }
                                            );
                                            executing = false;
                                        }
                                    },
                                    new DeploymentOptions().setWorker(deployType==DeployType.Blocking),
                                    asyncResultHandler
                            )
            );
        }

        logger.info("Spawned {} instances of {} test code", numberOfInstances, deployType);

        vertx.eventBus().publish(
                "maxrate.start",
                new byte[0]
        );

        startPeriodicRateComputation(histograms);

        return this;
    }

    public void wait(int msec) throws Exception
    {
        Thread.sleep(msec);
    }

}
