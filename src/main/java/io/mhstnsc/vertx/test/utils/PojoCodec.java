package io.mhstnsc.vertx.test.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;


public class PojoCodec implements MessageCodec
{

    @Override
    public void encodeToWire(Buffer buffer, Object o) {
    }

    @Override
    public Object decodeFromWire(int pos, Buffer buffer)
    {
        return null;
    }

    @Override
    public Object transform(Object o)
    {
        return o;
    }

    @Override
    public String name()
    {
        return "pojo";
    }

    @Override
    public byte systemCodecID()
    {
        return -1;
    }
}
