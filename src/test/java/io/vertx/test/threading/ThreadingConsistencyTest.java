package io.vertx.test.threading;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.junit.Timeout;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.vertx.test.testutils.AwaitUtils.awaitResult;


public class ThreadingConsistencyTest
{
    private Logger logger = LoggerFactory.getLogger(ThreadingConsistencyTest.class);

    private Vertx vertx;

    private CompletableFuture<AssertionError> assertions = new CompletableFuture<>();

    @Before
    public void setup() throws Exception
    {
        vertx = awaitResult(
                h->Vertx.clusteredVertx(
                new VertxOptions()
                        ,
                        h
                )
        );

        vertx.exceptionHandler(
                event ->
                {
                    assertions.complete((AssertionError)event);
                }
        );
    }



    @Test(expected= TimeoutException.class)
    public void testNoHandlersReply() throws Exception
    {

        String id = awaitResult(
                h->vertx.deployVerticle(
                        WorkerVerticle.class.getCanonicalName(),
                        new DeploymentOptions().setWorker(true).setInstances(1),
                        h
                )
        );

        assertions.get(1, TimeUnit.SECONDS);
    }
}
