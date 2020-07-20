package de.mkammerer.mrcanary.prometheus.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

// See src/test/resources/prometheus for example data
public class ResultParser {
    private final Gson gson = new Gson();

    public double parse(String body) {
        Map<String, Object> json = gson.fromJson(body, mapType());

        String status = getString(body, json, "status", "");
        if (!status.equals("success")) {
            throw new PrometheusException(String.format("Expected status 'success', got '%s'", status), body);
        }

        String resultType = getString(body, json, "data.resultType", "");
        if (!resultType.equals("vector")) {
            throw new PrometheusException(String.format("Expected data.resultType 'vector', got '%s'", resultType), body);
        }

        List<Map<String, Object>> result = getList(body, json, "data.result", List.of());

        if (result.isEmpty()) {
            throw new PrometheusException("Result is empty", body);
        }
        if (result.size() > 1) {
            throw new PrometheusException(String.format("Too many results. Expected 1, got %d", result.size()), body);
        }

        return extractResult(body, result.get(0));
    }

    @SuppressWarnings("unchecked")
    private double extractResult(String body, Map<String, Object> result) {
        Object value = result.get("value");
        if (value == null) {
            throw new PrometheusException("Found no value in result", body);
        }
        if (!(value instanceof List)) {
            throw new PrometheusException(String.format("Expected value to be of type list, but was %s", value.getClass()), body);
        }

        List<Object> valueList = (List<Object>) value;
        if (valueList.size() != 2) {
            throw new PrometheusException(String.format("Expected value to have 2 entries, but got %d", valueList.size()), body);
        }

        // Index 0 contains the timestamp
        Object realValue = valueList.get(1);
        if (realValue == null) {
            throw new PrometheusException("No real value found", body);
        }

        if (!(realValue instanceof String)) {
            throw new PrometheusException(String.format("Expected real value to be of type String, but was %s", realValue.getClass()), body);
        }

        if (realValue.equals("NaN")) {
            throw new PrometheusException("Real value is NaN", body);
        }

        return Double.parseDouble((String) realValue);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(String body, Map<String, Object> json, String key, List<Object> defaultValue) {
        Object value = locate(body, json, key, defaultValue);

        if (!(value instanceof List)) {
            throw new PrometheusException(String.format("Expected value of '%s' to be of type List, but was %s", key, value.getClass()), body);
        }

        return (List<Map<String, Object>>) value;
    }

    private String getString(String body, Map<String, Object> json, String key, String defaultValue) {
        Object value = locate(body, json, key, defaultValue);

        if (!(value instanceof String)) {
            throw new PrometheusException(String.format("Expected value of '%s' to be of type String, but was %s", key, value.getClass()), body);
        }

        return (String) value;
    }

    @SuppressWarnings("unchecked")
    private Object locate(String body, Map<String, Object> json, String key, @Nullable Object defaultValue) {
        int dot = key.indexOf('.');
        if (dot > -1) {
            // Descend into subtree
            String topKey = key.substring(0, dot);
            Object childJson = json.get(topKey);
            if (childJson == null) {
                throw new PrometheusException(String.format("No JSON found with key '%s'", key), body);
            }
            if (!(childJson instanceof Map)) {
                throw new PrometheusException(String.format("No JSON object found with key '%s'", key), body);
            }

            // Call ourself on subtree
            return locate(body, (Map<String, Object>) childJson, key.substring(dot + 1), defaultValue);
        } else {
            // Extract value
            Object value = json.getOrDefault(key, defaultValue);
            if (value == null) {
                return defaultValue;
            }

            return value;
        }
    }

    private Type mapType() {
        return new TypeToken<Map<String, Object>>() {
        }.getType();
    }
}
