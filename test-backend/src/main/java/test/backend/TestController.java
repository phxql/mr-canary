package test.backend;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ThreadLocalRandom;

@Controller(produces = MediaType.TEXT_PLAIN)
class TestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);

    private final String applicationName;
    private final Configuration configuration;
    private final MeterRegistry meterRegistry;

    @Inject
    public TestController(@Property(name = "micronaut.application.name") String applicationName, Configuration configuration, MeterRegistry meterRegistry) {
        this.applicationName = applicationName;
        this.configuration = configuration;
        this.meterRegistry = meterRegistry;
    }

    @Get
    public String index() {
        int dice = ThreadLocalRandom.current().nextInt(1, 100);

        // Current weight range from 0 to 100
        // if getFailurePercentage == 0 always false
        // if getFailurePercentage == 100 always true
        if (dice <= configuration.getFailurePercentage()) {
            throw new FailureException();
        }

        LOGGER.info("{}: Success", applicationName);
        meterRegistry.counter("test_backend_success", "name", applicationName).increment();
        meterRegistry.counter("test_backend_total", "name", applicationName).increment();
        return applicationName;
    }

    @Error(exception = FailureException.class)
    public HttpResponse<String> handleFailureException() {
        LOGGER.info("{}: Failure", applicationName);
        meterRegistry.counter("test_backend_failure", "name", applicationName).increment();
        meterRegistry.counter("test_backend_total", "name", applicationName).increment();
        return HttpResponse.serverError("Boom: " + applicationName);
    }
}
