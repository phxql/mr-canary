package de.mkammerer.mrcanary;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        LOGGER.info("Started");
        try {
            new Main().run(args);
        } catch (Exception e) {
            LOGGER.error("Unhandled exception, please report this as a bug", e);
        } finally {
            LOGGER.info("Stopped");
        }
    }

    private void run(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ClientChannelInitializer())
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            LOGGER.debug("Binding to port {} ...", PORT);
            ChannelFuture bind = bootstrap.bind(PORT).sync();
            LOGGER.info("Running on port {}", PORT);

            bind.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
