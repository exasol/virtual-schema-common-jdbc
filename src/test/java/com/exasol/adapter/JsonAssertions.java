package com.exasol.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import jakarta.json.*;

public final class JsonAssertions {
    private JsonAssertions() {
        // Utility class.
    }

    /** Matches objects recursively while allowing extra fields and ignoring array order. */
    public static void assertJsonEquals(final String expected, final String actual) {
        final JsonValue expectedValue = parse(expected);
        final JsonValue actualValue = parse(actual);
        assertTrue(matches(expectedValue, actualValue),
                () -> "Expected JSON " + expectedValue + " to match " + actualValue);
    }

    public static void assertJsonContains(final String expected, final String actual) {
        assertJsonEquals(expected, actual);
    }

    public static void assertJsonStrictEquals(final String expected, final String actual) {
        assertEquals(parse(expected), parse(actual));
    }

    private static JsonValue parse(final String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readValue();
        }
    }

    private static boolean matches(final JsonValue expected, final JsonValue actual) {
        if (actual == null || expected.getValueType() != actual.getValueType()) {
            return false;
        }
        switch (expected.getValueType()) {
            case OBJECT:
                final JsonObject expectedObject = expected.asJsonObject();
                final JsonObject actualObject = actual.asJsonObject();
                return expectedObject.entrySet().stream()
                        .allMatch(entry -> matches(entry.getValue(), actualObject.get(entry.getKey())));
            case ARRAY:
                final JsonArray expectedArray = expected.asJsonArray();
                final JsonArray actualArray = actual.asJsonArray();
                return expectedArray.size() == actualArray.size()
                        && matchesArray(expectedArray, actualArray, 0, new boolean[actualArray.size()]);
            default:
                return expected.equals(actual);
        }
    }

    private static boolean matchesArray(final JsonArray expected, final JsonArray actual, final int index,
            final boolean[] used) {
        if (index == expected.size()) {
            return true;
        }
        for (int actualIndex = 0; actualIndex < actual.size(); actualIndex++) {
            if (!used[actualIndex] && matches(expected.get(index), actual.get(actualIndex))) {
                used[actualIndex] = true;
                if (matchesArray(expected, actual, index + 1, used)) {
                    return true;
                }
                used[actualIndex] = false;
            }
        }
        return false;
    }
}
