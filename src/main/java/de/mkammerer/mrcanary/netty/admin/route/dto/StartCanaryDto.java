package de.mkammerer.mrcanary.netty.admin.route.dto;

import lombok.Value;

@Value
public
class StartCanaryDto {
    String canaryId;
    String status;
    int weight;
}
