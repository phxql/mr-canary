package de.mkammerer.mrcanary.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

final class NettyHelper {
    private NettyHelper() {
    }

    public static void flushAndClose(Channel channel) {
        if (channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * Returns the size of the given buffer.
     * <p>
     * If the buffer isn't a {@link ByteBuf}, it returns -1.
     *
     * @param buffer buffer
     * @return size of the buffer
     */
    public static int bufferSize(Object buffer) {
        if (buffer instanceof ByteBuf) {
            return ((ByteBuf) buffer).writableBytes();
        } else {
            return -1;
        }
    }
}
