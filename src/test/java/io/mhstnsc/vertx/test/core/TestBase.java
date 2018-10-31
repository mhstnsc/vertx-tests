package io.mhstnsc.vertx.test.core;

import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.CompletableFuture;

import static io.mhstnsc.vertx.test.utils.AwaitUtils.awaitResult;


public class TestBase
{
    private CompletableFuture<AssertionError> assertions = new CompletableFuture<>();

    private boolean useClusteredVertx;

    protected HazelcastClusterManager clusterManager;
    protected HazelcastInstance hazelcastInstance;

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
            clusterManager = new HazelcastClusterManager();

            VertxOptions vertxOptions = buildVertxOptions(
                    new VertxOptions()
                            .setClustered(true)
                            .setClusterManager(clusterManager)
            );


            vertx = awaitResult(
                    h -> Vertx.clusteredVertx(
                            vertxOptions,
                            h
                    )
            );

            hazelcastInstance = clusterManager.getHazelcastInstance();
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

    protected VertxOptions buildVertxOptions(VertxOptions options)
    {
        return options;
    }

    @After
    public void tearDown() throws Exception
    {
        vertx.close();
    }
}
