package io.mhstnsc.vertx.test.eventbus;

import com.hazelcast.config.Config;
import io.vertx.core.*;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.mhstnsc.vertx.test.utils.AwaitUtils.awaitResult;


public class SendToItselfBenchmark
{
    static ThreadMXBean threadMXBean;

    public static void main(String[] args)
    {
        String groupConfigName = "ClusteredVertxTest." + UUID.randomUUID().toString();

        HazelcastClusterManager hzClusterManager = new HazelcastClusterManager();
        Config cfgWithoutMulticast = hzClusterManager.loadConfig();
        cfgWithoutMulticast.getGroupConfig().setName(groupConfigName);

        threadMXBean = ManagementFactory.getThreadMXBean();
        threadMXBean.setThreadContentionMonitoringEnabled(true);

        VertxOptions vertxOptions = new VertxOptions()
                .setClustered(true)
                .setClusterHost("127.0.0.1")
                .setClusterManager(new HazelcastClusterManager(cfgWithoutMulticast));

        try
        {
            Vertx vertx = awaitResult(
                    h -> Vertx.clusteredVertx(
                            vertxOptions,
                            h
                    )
            );

            testSendToItself(vertx);

            vertx.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static public void testSendToItself(Vertx vertx) throws Exception
    {
        String consumerAddress = "testSendAndReply.consumerAddress";
        int numberOfVerticles = 32;

        // deploy the consumers

        byte[] payload = new byte[16];

        for (int i = 0; i < numberOfVerticles; i++)
        {
            String id = awaitResult(
                    h -> vertx.deployVerticle(
                            new AbstractVerticle()
                            {
                                @Override
                                public void start(Future<Void> startFuture) throws Exception
                                {
                                    vertx.eventBus().consumer(
                                            consumerAddress,
                                            event ->
                                            {
                                                vertx.eventBus().send(
                                                        consumerAddress,
                                                        payload
                                                );
                                            }
                                    ).completionHandler(
                                            startFuture.completer()
                                    );

                                    vertx.eventBus().send(
                                            consumerAddress,
                                            payload
                                    );
                                }
                            },
                            new DeploymentOptions(),
                            h
                    )
            );
        }

        Thread.sleep(10000);

        displayThreadInfo(threadMXBean);
    }

    static private void displayThreadInfo(ThreadMXBean threadMXBean)
    {
        List<Thread> threadIds = Thread.getAllStackTraces()
                                       .keySet()
                                       .stream()
                                       .filter(
                                               thread -> thread.getName().contains("vert.x-eventloop-thread")
                                       )
                                       .collect(Collectors.toList());

        for (Thread thread : threadIds)
        {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(thread.getId());

            System.out.println(String.format("ThreadInfo:%40s getBlockedTime:%5d msec getWaitedTime:%3d ",
                    thread.getName(),
                    threadInfo.getBlockedTime(),
                    threadInfo.getWaitedTime()
            ));
        }
    }

}
