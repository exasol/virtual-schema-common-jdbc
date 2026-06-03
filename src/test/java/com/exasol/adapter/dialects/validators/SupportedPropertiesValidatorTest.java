package com.exasol.adapter.dialects.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.properties.PropertyValidationException;

class SupportedPropertiesValidatorTest {
    @Test
    void testValidateSupportedProperty() {
        final SupportedPropertiesValidator validator = new SupportedPropertiesValidator().add(List.of("SUPPORTED"));
        assertDoesNotThrow(() -> validator.validate(new AdapterProperties(Map.of("SUPPORTED", "value"))));
    }

    @Test
    void testValidateUnsupportedProperty() {
        final SupportedPropertiesValidator validator = new SupportedPropertiesValidator().add(List.of("SUPPORTED"));
        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                () -> validator.validate(new AdapterProperties(Map.of("UNSUPPORTED", "value"))));
        assertThat(exception.getMessage(), equalTo(
                "E-VSCJDBC-13: This dialect does not support property 'UNSUPPORTED'. Please, do not set this property."));
    }

    @Test
    void testGetSupportedPropertiesReturnsDefensiveCopy() {
        final SupportedPropertiesValidator validator = new SupportedPropertiesValidator().add(List.of("SUPPORTED"));
        final Set<String> supportedProperties = validator.getSupportedProperties();

        assertAll(() -> assertThat(supportedProperties, containsInAnyOrder("SUPPORTED")),
                () -> assertThrows(UnsupportedOperationException.class, () -> supportedProperties.add("OTHER")),
                () -> assertDoesNotThrow(() -> validator.validate(new AdapterProperties(Map.of("SUPPORTED", "value")))));
    }
}
