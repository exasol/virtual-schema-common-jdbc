package com.exasol.adapter.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.properties.DataTypeDetection.Strategy;

class DataTypeDetectionTest {

    @Test
    void testFromProperties() {
        final DataTypeDetection.Strategy strategy = Strategy.EXASOL_CALCULATED;
        final AdapterProperties properties = adapterProperties(strategy.name());
        final DataTypeDetection testee = DataTypeDetection.from(properties);
        assertThat(testee.getStrategy(), equalTo(strategy));
        verifySuccess(properties);
    }

    @Test
    void testFromPropertiesFromResultSet() throws PropertyValidationException {
        final PropertyValidator validator = DataTypeDetection.getValidator();
        final Exception exception = assertThrows(PropertyValidationException.class,
                () -> validator.validate(adapterProperties("FROM_RESULT_SET")));
        assertThat(exception.getMessage(), equalTo(
                "E-VSCJDBC-47: Property `IMPORT_DATA_TYPES` value 'FROM_RESULT_SET' is no longer supported. Please remove the `IMPORT_DATA_TYPES` property from the virtual schema so the default value 'EXASOL_CALCULATED' is used."));
    }

    @Test
    void testUnsetProperty() {
        final AdapterProperties properties = new AdapterProperties(Map.of());
        final DataTypeDetection testee = DataTypeDetection.from(properties);
        assertThat(testee.getStrategy(), equalTo(DataTypeDetection.DEFAULT_STRATEGY));
        verifySuccess(properties);
    }

    @Test
    void testValidationFailure() throws PropertyValidationException {
        final PropertyValidator validator = DataTypeDetection.getValidator();
        final Exception exception = assertThrows(PropertyValidationException.class,
                () -> validator.validate(adapterProperties("invalid_value")));
        assertThat(exception.getMessage(),
                equalTo("E-VSCJDBC-41: Invalid value 'invalid_value' for property 'IMPORT_DATA_TYPES'."
                        + " Choose one of: FROM_RESULT_SET, EXASOL_CALCULATED."));
    }

    private void verifySuccess(final AdapterProperties properties) {
        assertDoesNotThrow(() -> DataTypeDetection.getValidator().validate(properties));
    }

    private AdapterProperties adapterProperties(final String value) {
        return new AdapterProperties(Map.of(DataTypeDetection.STRATEGY_PROPERTY, value));
    }
}
