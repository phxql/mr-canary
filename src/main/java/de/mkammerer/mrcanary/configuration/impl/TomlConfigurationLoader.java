package de.mkammerer.mrcanary.configuration.impl;

import com.moandjiezana.toml.Toml;
import de.mkammerer.mrcanary.configuration.CanaryConfiguration;
import de.mkammerer.mrcanary.configuration.ConfigurationException;
import de.mkammerer.mrcanary.configuration.ConfigurationLoader;
import de.mkammerer.mrcanary.configuration.GlobalConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
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
public class TomlConfigurationLoader implements ConfigurationLoader {
    private final Path file;

    @Override
    public GlobalConfiguration load() {
        Path configFile = file.toAbsolutePath();
        LOGGER.info("Loading configuration from '{}'", configFile);

        Toml toml = new Toml().read(configFile.toFile());
        Path canariesDirectory = Paths.get(toml.getString("canaries_directory")).toAbsolutePath();
        List<CanaryConfiguration> canaries = loadCanariesConfig(canariesDirectory);

        return new GlobalConfiguration(canariesDirectory, canaries);
    }

    private List<CanaryConfiguration> loadCanariesConfig(Path canariesDirectory) {
        LOGGER.debug("Loading canaries from '{}'", canariesDirectory);

        if (Files.notExists(canariesDirectory)) {
            LOGGER.debug("Directory doesn't exist");
            return List.of();
        }

        try (Stream<Path> canaryConfig = Files.list(canariesDirectory)) {
            return canaryConfig.map(this::loadCanaryConfig).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ConfigurationException(String.format("Failed to list canary configs from %s", canariesDirectory), e);
        }
    }

    private CanaryConfiguration loadCanaryConfig(Path file) {
        Path configFile = file.toAbsolutePath();
        LOGGER.info("Loading canary configuration from '{}'", configFile);

        Toml toml = new Toml().read(configFile.toFile());

        int port = Math.toIntExact(toml.getLong("port"));
        InetSocketAddress primaryAddress = parseAddress(toml.getString("primary_address"));
        InetSocketAddress canaryAddress = parseAddress(toml.getString("canary_address"));
        int maxFailures = Math.toIntExact(toml.getLong("max_failures"));
        Duration analysisInterval = Duration.parse(toml.getString("analysis_interval"));

        CanaryConfiguration.Prometheus prometheus = parsePrometheus(toml);
        CanaryConfiguration.Weight weight = parseWeight(toml);

        return new CanaryConfiguration(port, primaryAddress, canaryAddress, prometheus, maxFailures, analysisInterval, weight);
    }

    private CanaryConfiguration.Weight parseWeight(Toml toml) {
        int start = Math.toIntExact(toml.getLong("weight.start"));
        int end = Math.toIntExact(toml.getLong("weight.end"));
        int increase = Math.toIntExact(toml.getLong("weight.increase"));

        return new CanaryConfiguration.Weight(start, end, increase);
    }

    private CanaryConfiguration.Prometheus parsePrometheus(Toml toml) {
        String query = toml.getString("prometheus.query");
        Long min = toml.getLong("prometheus.min");
        Long max = toml.getLong("prometheus.max");

        return new CanaryConfiguration.Prometheus(query, min, max);
    }

    private InetSocketAddress parseAddress(String address) {
        // address is e.g. 'localhost:12345'
        String[] parts = address.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException(String.format("Expected 2 parts, got %d while parsing '%s'", parts.length, address));
        }

        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }
}
