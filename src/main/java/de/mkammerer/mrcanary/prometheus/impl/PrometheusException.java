package de.mkammerer.mrcanary.prometheus.impl;

import lombok.Getter;

class PrometheusException extends RuntimeException {
    @Getter
    private final String body;

    public PrometheusException(String message, String body) {
        super(message);
        this.body = body;
    }
}
