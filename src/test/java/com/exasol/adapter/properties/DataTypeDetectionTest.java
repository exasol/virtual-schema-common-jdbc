package com.exasol.adapter.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.exasol.adapter.AdapterProperties;

class DataTypeDetectionTest {

    @ParameterizedTest
    @CsvSource(value = { "FROM_RESULT_SET", "EXASOL_CALCULATED" })
    void testFromProperties(final DataTypeDetection.Strategy strategy) {
        final AdapterProperties properties = adapterProperties(strategy.name());
        final DataTypeDetection testee = DataTypeDetection.from(properties);
        assertThat(testee.getStrategy(), equalTo(strategy));
        verifyValidator(properties);
    }

    @Test
    void testUnsetProperty() {
        final AdapterProperties properties = new AdapterProperties(Map.of());
        final DataTypeDetection testee = DataTypeDetection.from(properties);
        assertThat(testee.getStrategy(), equalTo(DataTypeDetection.DEFAULT_STRATEGY));
        verifyValidator(properties);
    }

    @Test
    void testValidationFailure() throws PropertyValidationException {
        final PropertyValidator validator = DataTypeDetection.getValidator();
        assertThrows(PropertyValidationException.class, () -> validator.validate(adapterProperties("invalid_value")));
    }

    private void verifyValidatedSuccessfully(final AdapterProperties properties) {
        assertDoesNotThrow(() -> DataTypeDetection.getValidator().validate(properties));
    }

    private AdapterProperties adapterProperties(final String value) {
        return new AdapterProperties(Map.of(DataTypeDetection.STRATEGY_PROPERTY, value));
    }
}
