package io.mhstnsc;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;

import static java.nio.file.StandardOpenOption.READ;


public class SeekableChannelTest
{
    @Test
    public void testReadBeyondEnd() throws Exception
    {
        try (SeekableByteChannel channel = Files.newByteChannel(Paths.get("readbeyondend.txt"),EnumSet.of(READ)))
        {
            channel.position(100000);

            ByteBuffer buf = ByteBuffer.allocate(10);
            while (buf.remaining() > 0)
            {
                channel.read(buf);
            }
            buf.flip();
        }
    }
}
