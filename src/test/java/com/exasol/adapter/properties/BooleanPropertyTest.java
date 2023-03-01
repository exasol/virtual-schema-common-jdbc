package com.exasol.adapter.properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exasol.adapter.AdapterProperties;

class BooleanPropertyTest {

    static private final String PROPERTY = "MY_PROPERTY";

    @Test
    void testFailure() {
        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                () -> verify("123"));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-15"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "true", "True", "TRUE", "false", "False", "FALSE" })
    void testSuccess(final String value) {
        assertDoesNotThrow(() -> verify(value));
    }

    private void verify(final String value) throws PropertyValidationException {
        final AdapterProperties properties = new AdapterProperties(Map.of(PROPERTY, value));
        BooleanProperty.validator(PROPERTY).validate(properties);
    }

}
