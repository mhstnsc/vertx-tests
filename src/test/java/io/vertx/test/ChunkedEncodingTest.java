package io.vertx.test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.test.core.VertxTestBase;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;


public class ChunkedEncodingTest extends VertxTestBase
{
    private Router router;

    @Before
    public void setup() throws Exception
    {
        HttpServer httpServer = vertx.createHttpServer();

        router = Router.router(vertx);

        HttpServer aServer = awaitResult(
                asyncResultHandler ->
                        httpServer
                                .requestHandler(router::accept)
                                .listen(8081, "127.0.0.1", asyncResultHandler)
        );
    }

    @Test
    public void missingNewLineAfterSize() throws Exception
    {
        // connect to server

        NetClient netClient = vertx.createNetClient(
                new NetClientOptions()
                        .setLogActivity(true)
        );

        NetSocket socket = awaitResult(
                asyncResultHandler ->
                        netClient.connect(
                                8081,
                                "127.0.0.1",
                                asyncResultHandler
                        )
        );

        String httpRequest =
                "GET / HTTP/1.1\r\n" +
                        "Transfer-Encoding: chunked\r\n" +
                        "\r\n" +
                        "5\r\n" +
                        "buggy\r\n" +
                        "4" + // malformed here (missing \r\n)
                        " not\r\n" +
                        "\r\n";

        CompletableFuture<String> requestBody = new CompletableFuture<>();

        router.route().handler(BodyHandler.create());
        router.route().handler(
                event ->
                {
                    requestBody.complete(event.getBodyAsString());
                    event.response().setStatusCode(200);
                    event.response().end();
                }
        );


        CompletableFuture<Buffer> response = new CompletableFuture<>();

        socket.write(Buffer.buffer(httpRequest.getBytes()));

        String requestBodyData = requestBody.get(5, TimeUnit.SECONDS);
        assertEquals("buggy not", requestBodyData);
    }

    @SuppressWarnings("unchecked")
    public static <T> T awaitResult(Consumer<Handler<AsyncResult<T>>> consumer) throws InterruptedException, ExecutionException, TimeoutException {
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
