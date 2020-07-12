package de.mkammerer.mrcanary.configuration.impl;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DurationTest {
    @Test
    void parse() {
        assertThat(Duration.parse("PT30S").getSeconds()).isEqualTo(30);
        assertThat(Duration.parse("PT1M").getSeconds()).isEqualTo(60);
        assertThat(Duration.parse("PT1H").getSeconds()).isEqualTo(60 * 60);
    }
}