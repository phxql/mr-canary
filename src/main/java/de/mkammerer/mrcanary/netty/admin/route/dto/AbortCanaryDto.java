package de.mkammerer.mrcanary.netty.admin.route.dto;

import lombok.Value;

@Value
public
class AbortCanaryDto {
    String canaryId;
    String status;
    String currentBackend;
}
