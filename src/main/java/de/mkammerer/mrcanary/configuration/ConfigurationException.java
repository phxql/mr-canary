package de.mkammerer.mrcanary.configuration;

public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
