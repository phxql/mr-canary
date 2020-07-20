package de.mkammerer.mrcanary.prometheus.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultParserTest {
    private ResultParser sut = new ResultParser();

    @Test
    void empty() throws IOException {
        String body = loadData("/prometheus/empty.json");

        assertThatThrownBy(() ->
            sut.parse(body)
        ).isInstanceOf(PrometheusException.class).hasMessageContaining("Result is empty");
    }

    @Test
    void multiple() throws IOException {
        String body = loadData("/prometheus/multiple.json");

        assertThatThrownBy(() ->
            sut.parse(body)
        ).isInstanceOf(PrometheusException.class).hasMessageContaining("Too many results");
    }

    @Test
    void nan() throws IOException {
        String body = loadData("/prometheus/nan.json");

        assertThatThrownBy(() ->
            sut.parse(body)
        ).isInstanceOf(PrometheusException.class).hasMessageContaining("NaN");
    }

    @Test
    void success() throws IOException {
        String body = loadData("/prometheus/success.json");

        assertThat(sut.parse(body)).isEqualTo(0.41666666666666663);
    }

    private String loadData(String path) throws IOException {
        try (InputStream stream = ResultParser.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException(String.format("Failed to load resource '%s'", path));
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}