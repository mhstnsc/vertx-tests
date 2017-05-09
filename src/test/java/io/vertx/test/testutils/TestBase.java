package io.vertx.test.testutils;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static io.mhstnsc.vertx.test.AwaitUtils.awaitResult;


public class TestBase
{
    protected Logger logger = LoggerFactory.getLogger(getClass());

    private CompletableFuture<AssertionError> assertions = new CompletableFuture<>();

    protected Vertx vertx;

    protected void startVertx(VertxOptions vertxOptions) throws Exception
    {
        vertx = awaitResult(
                h -> Vertx.clusteredVertx(
                        vertxOptions,
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
}
