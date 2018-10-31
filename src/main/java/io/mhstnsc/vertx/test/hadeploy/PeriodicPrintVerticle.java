package io.mhstnsc.vertx.test.hadeploy;

import io.vertx.core.AbstractVerticle;


public class PeriodicPrintVerticle extends AbstractVerticle
{
    @Override
    public void start() throws Exception
    {
        super.start();
        vertx.setPeriodic(1000, event -> System.out.println("i'm alive"));
    }
}
