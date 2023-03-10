package com.exasol.adapter.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterProperties;

class SchemaNamePropertyTest {
    @Test
    void testEmpty() {
        final PropertyValidator testee = SchemaNameProperty.validator("Spanish");
        final Exception exception = assertThrows(PropertyValidationException.class,
                () -> testee.validate(AdapterProperties.emptyProperties()));
        assertThat(exception.getMessage(),
                equalTo("E-VSCJDBC-45: Spanish virtual schema dialect requires to specify a schema name."
                        + " Please specify a schema name using property 'SCHEMA_NAME'."));
    }

    @Test
    void testNonEmpty() {
        final PropertyValidator testee = SchemaNameProperty.validator("Spanish");
        assertDoesNotThrow(() -> testee.validate(adapterProperties("SCHEMA_NAME", "abc")));
    }

    private AdapterProperties adapterProperties(final String key, final String value) {
        return new AdapterProperties(Map.of(key, value));
    }
}
