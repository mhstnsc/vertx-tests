package io.vertx.test.performance;

import com.hazelcast.util.MD5Util;
import io.vertx.core.*;
import io.vertx.test.testutils.MaxRateExecutor;
import io.vertx.test.testutils.TestBase;
import org.junit.Before;
import org.junit.Test;

import java.security.MessageDigest;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import static io.vertx.test.testutils.AwaitUtils.awaitResult;
import static org.junit.Assert.assertTrue;


@SuppressWarnings("unused")
public class EventBusTest extends TestBase
{
    @Before
    public void setup() throws Exception
    {
        startVertx(new VertxOptions()
                .setWorkerPoolSize(400)
                .setInternalBlockingPoolSize(400)
                .setEventLoopPoolSize(4 * Runtime.getRuntime().availableProcessors())
        );
    }

    /**
     * Send and reply between verticles as fast as possible and measure rate
     *
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void testWorkerConsumer() throws Exception
    {
        String address = "testWorkerConsumer";
        int numberOfConsumers = 200;
        int numberOfSenders = 2000;
        boolean useWorkerConsumers = true;

        // deploy the consumers

        for(int i=0; i< numberOfConsumers; i++)
        {
            String id = awaitResult(
                    h-> vertx.deployVerticle(
                            new AbstractVerticle()
                            {
                                @Override
                                public void start(Future<Void> startFuture) throws Exception
                                {
                                    vertx.eventBus().consumer(
                                            address,
                                            event -> event.reply(null)
                                    ).completionHandler(
                                            startFuture.completer()
                                    );
                                }
                            },
                            new DeploymentOptions().setWorker(useWorkerConsumers),
                            h
                    )
            );
        }

        // deploy the senders and start generating traffic at maximum speed

        MaxRateExecutor.create()
                       .start(
                               vertx,
                               numberOfSenders,
                               MaxRateExecutor.DeployType.Async,
                               () -> new MaxRateExecutor.TestedCode()
                               {
                                   @Override
                                   public void init(Future<Void> future)
                                   {
                                       future.complete();
                                   }

                                   @Override
                                   public void run(Runnable finished)
                                   {
                                       vertx.eventBus().send(
                                               address,
                                               new byte[16],
                                               event ->
                                               {
                                                   assertTrue(event.succeeded());
                                                   finished.run();
                                               }
                                       );
                                   }
                               }
                       );

        Thread.sleep(200000);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testMaxCpuLoad() throws Exception
    {
        String address = "testWorkerConsumer";
        int numberOfConsumers = 200;
        int numberOfSenders = 200;
        boolean useWorkerConsumers = true;


        AtomicLong atomicLong = new AtomicLong();

        MaxRateExecutor.create()
                       .start(
                               vertx,
                               numberOfSenders,
                               MaxRateExecutor.DeployType.Blocking,
                               () -> new MaxRateExecutor.TestedCode()
                               {
                                   @Override
                                   public void init(Future<Void> future)
                                   {
                                       future.complete();
                                   }

                                   @Override
                                   public void run(Runnable finished)
                                   {
                                       byte[] data = new byte[1024];
                                       atomicLong.set(MD5Util.toMD5String(new String(data)).length());

                                       finished.run();
                                   }
                               }
                       );

        Thread.sleep(200000);
    }
}
