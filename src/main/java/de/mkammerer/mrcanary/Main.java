package de.mkammerer.mrcanary;

import de.mkammerer.mrcanary.canary.Canary;
import de.mkammerer.mrcanary.canary.CanaryManager;
import de.mkammerer.mrcanary.configuration.ConfigurationLoader;
import de.mkammerer.mrcanary.configuration.GlobalConfiguration;
import de.mkammerer.mrcanary.configuration.impl.TomlConfigurationLoader;
import de.mkammerer.mrcanary.netty.ReverseProxyInitializer;
import de.mkammerer.mrcanary.prometheus.Prometheus;
import de.mkammerer.mrcanary.prometheus.impl.PrometheusMock;
import de.mkammerer.mrcanary.util.NamedThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@RequiredArgsConstructor
public final class Main {
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
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("canary-analyzer-%d"));
        try {
            Prometheus prometheus = new PrometheusMock(0, 20);
            CanaryManager canaryManager = CanaryManager.fromConfiguration(globalConfiguration.getCanaries(), scheduler, prometheus);

            List<ChannelFuture> futures = new ArrayList<>(globalConfiguration.getCanaries().size());
            for (Canary canary : canaryManager.getCanaries()) {
                futures.add(startNetty(bossGroup, workerGroup, canary));
            }

            // Wait for all channels to close - this will not happen unless process is being killed
            for (ChannelFuture future : futures) {
                future.channel().closeFuture().sync();
            }
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            scheduler.shutdownNow();
        }
    }

    private ChannelFuture startNetty(EventLoopGroup bossGroup, EventLoopGroup workerGroup, Canary canary) throws InterruptedException {
        LOGGER.info("Starting canary '{}' on port {} ...", canary.getName(), canary.getPort());

        ServerBootstrap bootstrap = new ServerBootstrap();
        ChannelFuture future = bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ReverseProxyInitializer(canary))
            .childOption(ChannelOption.AUTO_READ, false)
            .bind(canary.getPort()).sync();
        LOGGER.info("Canary '{}' is running on port {}", canary.getName(), canary.getPort());

        return future;
    }
}