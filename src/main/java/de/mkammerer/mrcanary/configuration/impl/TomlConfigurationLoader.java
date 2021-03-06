package de.mkammerer.mrcanary.configuration.impl;

import com.moandjiezana.toml.Toml;
import de.mkammerer.mrcanary.canary.CanaryId;
import de.mkammerer.mrcanary.configuration.CanaryConfiguration;
import de.mkammerer.mrcanary.configuration.ConfigurationException;
import de.mkammerer.mrcanary.configuration.ConfigurationLoader;
import de.mkammerer.mrcanary.configuration.GlobalConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
// TODO: Throw useful exceptions on invalid data
// TODO: Validate id
public class TomlConfigurationLoader implements ConfigurationLoader {
    private final Path file;

    @Override
    public GlobalConfiguration load() {
        Path configFile = file.toAbsolutePath();
        LOGGER.info("Loading configuration from '{}'", configFile);

        Toml toml = new Toml().read(configFile.toFile());
        Path canariesDirectory = Paths.get(toml.getString("canaries_directory")).toAbsolutePath();
        InetSocketAddress adminAddress = parseAddress(toml.getString("admin_address"));
        List<CanaryConfiguration> canaries = loadCanariesConfig(canariesDirectory);

        return new GlobalConfiguration(canariesDirectory, adminAddress, canaries);
    }

    private List<CanaryConfiguration> loadCanariesConfig(Path canariesDirectory) {
        LOGGER.debug("Loading canaries from '{}'", canariesDirectory);

        if (Files.notExists(canariesDirectory)) {
            LOGGER.debug("Directory doesn't exist");
            return List.of();
        }

        try (Stream<Path> canaryConfig = Files.list(canariesDirectory).sorted()) {
            return canaryConfig.map(this::loadCanaryConfig).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ConfigurationException(String.format("Failed to list canary configs from %s", canariesDirectory), e);
        }
    }

    private CanaryConfiguration loadCanaryConfig(Path file) {
        Path configFile = file.toAbsolutePath();
        LOGGER.info("Loading canary configuration from '{}'", configFile);

        Toml toml = new Toml().read(configFile.toFile());

        String id = toml.getString("id");
        int port = Math.toIntExact(toml.getLong("port"));
        InetSocketAddress blueAddress = parseAddress(toml.getString("blue_address"));
        InetSocketAddress greenAddress = parseAddress(toml.getString("green_address"));
        int maxFailures = Math.toIntExact(toml.getLong("max_failures"));
        Duration analysisInterval = Duration.parse(toml.getString("analysis_interval"));

        CanaryConfiguration.PrometheusConfiguration prometheus = parsePrometheus(toml);
        CanaryConfiguration.WeightConfiguration weight = parseWeight(toml);

        return new CanaryConfiguration(CanaryId.of(id), port, blueAddress, greenAddress, prometheus, maxFailures, analysisInterval, weight);
    }

    private CanaryConfiguration.WeightConfiguration parseWeight(Toml toml) {
        int start = Math.toIntExact(toml.getLong("weight.start"));
        int end = Math.toIntExact(toml.getLong("weight.end"));
        int increase = Math.toIntExact(toml.getLong("weight.increase"));

        return new CanaryConfiguration.WeightConfiguration(start, end, increase);
    }

    private CanaryConfiguration.PrometheusConfiguration parsePrometheus(Toml toml) {
        URI blueUri = URI.create(toml.getString("prometheus.blue_uri"));
        URI greenUri = URI.create(toml.getString("prometheus.green_uri"));
        String blueQuery = toml.getString("prometheus.blue_query");
        String greenQuery = toml.getString("prometheus.green_query");
        Double min = toml.getDouble("prometheus.min");
        Double max = toml.getDouble("prometheus.max");

        return new CanaryConfiguration.PrometheusConfiguration(blueUri, greenUri, blueQuery, greenQuery, min, max);
    }

    private InetSocketAddress parseAddress(String address) {
        // address is e.g. 'localhost:12345'

        int colonIndex = address.lastIndexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException(String.format("Failed to find port in '%s'", address));
        }

        String hostname = address.substring(0, colonIndex);
        int port = Integer.parseInt(address.substring(colonIndex + 1));

        return new InetSocketAddress(hostname, port);
    }
}
