package io.mhstnsc.vertx.test.testutils;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static io.mhstnsc.vertx.test.AwaitUtils.awaitResult;


public class TestBase
{
    private CompletableFuture<AssertionError> assertions = new CompletableFuture<>();

    private boolean useClusteredVertx;

    protected Vertx vertx;

    public TestBase()
    {
        this.useClusteredVertx = false;
    }

    protected TestBase(boolean useClusteredVertx)
    {
        this.useClusteredVertx = useClusteredVertx;
    }

    @Before
    public void setup() throws Exception
    {
        if(useClusteredVertx)
        {
            VertxOptions vertxOptions = new VertxOptions()
                    .setClustered(true)
                    .setClusterManager(new HazelcastClusterManager());

            vertx = awaitResult(
                    h -> Vertx.clusteredVertx(
                            vertxOptions,
                            h
                    )
            );
        }
        else
        {
            vertx = Vertx.vertx();
        }


        vertx.exceptionHandler(
                event ->
                {
                    assertions.complete((AssertionError) event);
                }
        );
    }

    @After
    public void tearDown() throws Exception
    {
        vertx.close();
    }
}
