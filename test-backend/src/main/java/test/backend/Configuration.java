package test.backend;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@ConfigurationProperties("service")
class Configuration {
    private final int failurePercentage;

    @ConfigurationInject
    public Configuration(@Min(0) @Max(100) int failurePercentage) {
        this.failurePercentage = failurePercentage;
    }

    public int getFailurePercentage() {
        return failurePercentage;
    }
}
