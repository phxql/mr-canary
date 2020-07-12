package de.mkammerer.mrcanary.configuration;

import lombok.Value;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;

@Value
public class GlobalConfiguration {
    Path canariesDirectory;
    InetSocketAddress adminAddress;

    List<CanaryConfiguration> canaries;
}
