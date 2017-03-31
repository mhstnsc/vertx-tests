package io.vertx.test.threading;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class WorkerVerticle extends AbstractVerticle
{
    private Logger logger = LoggerFactory.getLogger(ThreadingConsistencyTest.class);

    @Override
    public void start() throws Exception
    {
        logger.debug("{} {}", Thread.currentThread(), Vertx.currentContext());

        Context expectedContext = Vertx.currentContext();
        //assertTrue(Context.isOnWorkerThread());

        vertx.eventBus().send(
                "bla",
                new byte[0],
                event ->
                {
                    new Throwable().printStackTrace();

                    logger.debug("{} {}", Thread.currentThread(), Vertx.currentContext(), event.cause());
                    assertEquals(expectedContext, Vertx.currentContext());
//                    assertTrue(Context.isOnWorkerThread());
                }
        );
    }
}
