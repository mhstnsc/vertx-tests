package io.mhstnsc.vertx.test.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;


public class AwaitUtils
{
    @SuppressWarnings("unchecked")
    public static <T> T awaitResult(Consumer<Handler<AsyncResult<T>>> consumer) throws InterruptedException, ExecutionException, TimeoutException
    {
        CompletableFuture future = new CompletableFuture();
        consumer.accept((h) -> {
            if(h.succeeded()) {
                future.complete(h.result());
            } else {
                future.completeExceptionally(h.cause());
            }
        });
        return (T) future.get(10000, TimeUnit.MILLISECONDS);
    }
}
