package de.mkammerer.mrcanary;

import de.mkammerer.mrcanary.configuration.ConfigurationLoader;
import de.mkammerer.mrcanary.configuration.GlobalConfiguration;
import de.mkammerer.mrcanary.configuration.impl.TomlConfigurationLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RequiredArgsConstructor
public final class Main {
    private static final int LOCAL_PORT = 8080;
    private static final SocketAddress BACKEND = new InetSocketAddress("www.mkammerer.de", 443);
    private static final Path CONFIG_FILE = Paths.get("config.toml");

    private final ConfigurationLoader configurationLoader;

    public static void main(String[] args) {
        LOGGER.info("Started");
        try {
            new Main(new TomlConfigurationLoader(CONFIG_FILE)).run(args);
        } catch (Exception e) {
            LOGGER.error("Unhandled exception, please report this as a bug", e);
            System.exit(1);
        } finally {
            LOGGER.info("Stopped");
        }
    }

    private void run(String[] args) throws InterruptedException {
        GlobalConfiguration globalConfiguration = configurationLoader.load();
        LOGGER.info("Using configuration {}", globalConfiguration);

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            ChannelFuture future = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ReverseProxyInitializer(BACKEND))
                .childOption(ChannelOption.AUTO_READ, false)
                .bind(LOCAL_PORT).sync();

            LOGGER.info("Running on port {}", LOCAL_PORT);

            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}