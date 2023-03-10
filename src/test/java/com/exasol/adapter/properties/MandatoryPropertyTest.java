package com.exasol.adapter.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterProperties;

class MandatoryPropertyTest {
    @Test
    void testEmpty() {
        final Exception exception = assertThrows(PropertyValidationException.class,
                () -> testee().validate(AdapterProperties.emptyProperties()));
        assertThat(exception.getMessage(),
                equalTo("E-VSCJDBC-45: Klingon virtual schema dialect requires to specify a mandatory element."
                        + " Please specify a mandatory element using property 'PROPERTY'."));
    }

    @Test
    void testNonEmpty() {
        assertDoesNotThrow(() -> testee().validate(adapterProperties("SCHEMA_NAME", "abc")));
    }

    private PropertyValidator testee() {
        return MandatoryProperty.validator("Klingon", "mandatory element", "PROPERTY");
    }

    private AdapterProperties adapterProperties(final String key, final String value) {
        return new AdapterProperties(Map.of(key, value));
    }
}
