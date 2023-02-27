package com.exasol.adapter.properties;

import static com.exasol.adapter.AdapterProperties.IS_LOCAL_PROPERTY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterProperties;

class BooleanPropertyTest {
    @Test
    void testValidateBooleanProperty() {
        final AdapterProperties adapterProperties = new AdapterProperties(Map.of(IS_LOCAL_PROPERTY, "123"));
        final PropertyValidator testee = BooleanProperty.validator(IS_LOCAL_PROPERTY);
        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                () -> testee.validate(adapterProperties));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-15"));
    }
}
