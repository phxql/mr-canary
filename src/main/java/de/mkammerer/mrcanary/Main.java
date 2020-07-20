package de.mkammerer.mrcanary;

import de.mkammerer.mrcanary.canary.Canary;
import de.mkammerer.mrcanary.canary.CanaryManager;
import de.mkammerer.mrcanary.canary.state.CanaryStateManager;
import de.mkammerer.mrcanary.canary.state.impl.InMemoryCanaryStateManager;
import de.mkammerer.mrcanary.configuration.ConfigurationLoader;
import de.mkammerer.mrcanary.configuration.GlobalConfiguration;
import de.mkammerer.mrcanary.configuration.impl.TomlConfigurationLoader;
import de.mkammerer.mrcanary.netty.ReverseProxyInitializer;
import de.mkammerer.mrcanary.netty.admin.AdminInitializer;
import de.mkammerer.mrcanary.netty.admin.Routes;
import de.mkammerer.mrcanary.netty.admin.route.impl.AbortCanaryRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.CanariesRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.DefaultRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.StartCanaryRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.StatusRoute;
import de.mkammerer.mrcanary.prometheus.Prometheus;
import de.mkammerer.mrcanary.prometheus.impl.NettyPrometheus;
import de.mkammerer.mrcanary.prometheus.impl.ResultParser;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
        try {
            Prometheus prometheus = new NettyPrometheus(new ResultParser(), workerGroup);
            CanaryStateManager canaryStateManager = new InMemoryCanaryStateManager();
            CanaryManager canaryManager = CanaryManager.fromConfiguration(globalConfiguration.getCanaries(), workerGroup, prometheus, canaryStateManager);
            Routes routes = new Routes(
                new DefaultRoute(),
                new StatusRoute(),
                new CanariesRoute(canaryManager),
                new StartCanaryRoute(canaryManager),
                new AbortCanaryRoute(canaryManager)
            );

            List<ChannelFuture> futures = new ArrayList<>(globalConfiguration.getCanaries().size());

            // Add admin ports
            futures.add(startNettyForAdmin(bossGroup, workerGroup, globalConfiguration.getAdminAddress(), routes));

            // Add canary ports
            for (Canary canary : canaryManager.getCanaries()) {
                futures.add(startNettyForCanary(bossGroup, workerGroup, canary));
            }

            // Wait for all channels to close - this will not happen unless process is being killed
            for (ChannelFuture future : futures) {
                future.channel().closeFuture().sync();
            }
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private ChannelFuture startNettyForAdmin(EventLoopGroup bossGroup, EventLoopGroup workerGroup, InetSocketAddress adminAddress, Routes routes) throws InterruptedException {
        LOGGER.info("Starting admin interface on {}", adminAddress);

        ServerBootstrap bootstrap = new ServerBootstrap();

        return bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new AdminInitializer(routes))
            .bind(adminAddress).sync();
    }

    private ChannelFuture startNettyForCanary(EventLoopGroup bossGroup, EventLoopGroup workerGroup, Canary canary) throws InterruptedException {
        LOGGER.info("Starting canary '{}' on port {} ...", canary.getId(), canary.getPort());

        ServerBootstrap bootstrap = new ServerBootstrap();
        ChannelFuture future = bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ReverseProxyInitializer(canary))
            .childOption(ChannelOption.AUTO_READ, false)
            .bind(canary.getPort()).sync();
        LOGGER.info("Canary '{}' is running on port {}", canary.getId(), canary.getPort());

        return future;
    }
}