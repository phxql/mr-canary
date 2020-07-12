package de.mkammerer.mrcanary.configuration;

import lombok.Value;

import java.nio.file.Path;
import java.util.List;

@Value
public class GlobalConfiguration {
    Path canariesDirectory;

    List<CanaryConfiguration> canaries;
}
