package de.mkammerer.mrcanary.netty.admin.route.dto;

import de.mkammerer.mrcanary.canary.Canary;
import de.mkammerer.mrcanary.canary.state.CanaryState;
import lombok.Value;

import java.util.List;

@Value(staticConstructor = "of")
public class CanariesDto {
    List<CanaryDto> canaries;

    @Value
    public static class CanaryDto {
        String id;
        String blueBackend;
        String greenBackend;
        String status;
        String currentBackend;
        int weight;
        int failures;

        public static CanaryDto fromCanary(Canary canary) {
            CanaryState state = canary.getState();

            return new CanariesDto.CanaryDto(
                canary.getId().toString(),
                canary.getBlueAddress().toString(), canary.getGreenAddress().toString(), state.getStatus().toString(),
                state.getStatus().getBackendColor().toString(), state.getWeight(), state.getFailures()
            );
        }
    }
}
