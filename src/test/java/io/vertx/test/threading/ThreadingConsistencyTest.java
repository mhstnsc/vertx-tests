package io.vertx.test.threading;

import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.mstnsc.vertx.test.AwaitUtils.awaitResult;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ThreadingConsistencyTest
{
    private Logger logger = LoggerFactory.getLogger(ThreadingConsistencyTest.class);

    private Vertx vertx;

    private CompletableFuture<AssertionError> assertions = new CompletableFuture<>();

    @Before
    public void setup() throws Exception
    {
        vertx = awaitResult(
                h -> Vertx.clusteredVertx(
                        new VertxOptions()
                        ,
                        h
                )
        );

        vertx.exceptionHandler(
                event ->
                {
                    assertions.complete((AssertionError) event);
                }
        );
    }


    @Test(expected = TimeoutException.class)
    public void testNoHandlersReply_FromWorkerVerticle() throws Exception
    {

        String id = awaitResult(
                h -> vertx.deployVerticle(
                        WorkerVerticle.class.getCanonicalName(),
                        new DeploymentOptions().setWorker(false).setInstances(1),
                        h
                )
        );

        assertions.get(1, TimeUnit.SECONDS);
    }

    @Test
    public void testSendTimeout_FromWorkerVerticle() throws Exception
    {
        // another verticle which does not reply

        String id = awaitResult(
                h -> vertx.deployVerticle(
                        new AbstractVerticle()
                        {
                            @Override
                            public void start(Future<Void> startFuture) throws Exception
                            {
                                vertx.eventBus().consumer(
                                        "testSendTimeoutAddress",
                                        event ->
                                        {
                                        }
                                ).completionHandler(event -> h.handle(Future.succeededFuture()));
                                super.start(startFuture);
                            }
                        },
                        new DeploymentOptions().setWorker(false).setInstances(1),
                        h
                )
        );

        // send the message and complete after we got the timeout

        Void aVoid = awaitResult(
                h -> vertx.deployVerticle(
                        new AbstractVerticle()
                        {
                            @Override
                            public void start(Future<Void> startFuture) throws Exception
                            {
                                Context expectedContext = Vertx.currentContext();

                                vertx.eventBus().send(
                                        "testSendTimeoutAddress",
                                        "dudu",
                                        new DeliveryOptions().setSendTimeout(10),
                                        event ->
                                        {
                                            if (event.failed() &&
                                                    ((ReplyException) event.cause())
                                                            .failureType() == ReplyFailure.TIMEOUT)
                                            {
                                                assertEquals(expectedContext, Vertx.currentContext());
                                                assertTrue(Context.isOnWorkerThread());

                                                h.handle(Future.succeededFuture());
                                            }
                                            else
                                            {
                                                h.handle(Future.failedFuture(new Exception(event.cause())));
                                            }
                                        }
                                );
                                startFuture.complete();
                            }
                        },
                        new DeploymentOptions().setWorker(true),
                        event ->
                        {
                        }
                )
        );
    }
}
