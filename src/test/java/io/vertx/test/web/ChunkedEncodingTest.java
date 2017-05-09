package io.vertx.test.web;

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
import java.util.concurrent.TimeUnit;

import static io.mhstnsc.vertx.test.AwaitUtils.awaitResult;


public class ChunkedEncodingTest extends VertxTestBase
{
    private Router router;
    private HttpServer httpServer;

    @Before
    public void setup() throws Exception
    {
        httpServer = vertx.createHttpServer();

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
                        "Connection: close\r\n" +
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

        CompletableFuture<Void> responseEnd = new CompletableFuture<>();

        socket.write(Buffer.buffer(httpRequest.getBytes()));
//        socket.handler(event ->
//                {
//                    response.appendBuffer(event);
//                }
//        );
        socket.endHandler(event -> responseEnd.complete(null));

        responseEnd.get(5, TimeUnit.SECONDS);

        String requestBodyData = requestBody.get(5, TimeUnit.SECONDS);
        assertEquals("buggy not", requestBodyData);
    }
}
