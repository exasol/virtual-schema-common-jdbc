package com.exasol.adapter.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect.StructureElementSupport;
import com.exasol.adapter.properties.PropertyValidator.PropertyValueValidator;

class PropertyValidatorTest {
    static final String ELEMENT_NAME = "sample-element-name";
    static final String PROPERTY = "MY_PROPERTY";

    @Test
    void testOptional() throws PropertyValidationException {
        final PropertyValueValidator valueValidator = mock(PropertyValueValidator.class);
        final PropertyValidator testee = PropertyValidator.optional(PROPERTY, valueValidator);
        testee.validate(adapterProperties(""));
        verify(valueValidator).validate("");
        testee.validate(adapterProperties("a"));
        verify(valueValidator).validate("a");
    }

    @Test
    void testIgnoreEmpty() throws PropertyValidationException {
        final PropertyValueValidator valueValidator = mock(PropertyValueValidator.class);
        final PropertyValidator testee = PropertyValidator.ignoreEmpty(PROPERTY, valueValidator);
        testee.validate(adapterProperties(""));
        verifyNoInteractions(valueValidator);
        testee.validate(adapterProperties("a"));
        verify(valueValidator).validate("a");
    }

    @Test
    void testUnsupportedStructureElement() {
        final PropertyValidator testee = forStructureElement(StructureElementSupport.NONE);
        assertDoesNotThrow(() -> testee.validate(new AdapterProperties(Map.of())));
        assertThrows(PropertyValidationException.class, () -> testee.validate(adapterProperties("")));
        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                () -> testee.validate(adapterProperties("abc")));
        assertThat(exception.getMessage(), equalTo("E-VSCJDBC-44:" //
                + " This dialect does not support 'sample-element-name'." //
                + " Please, do not set property 'MY_PROPERTY'."));
    }

    @ParameterizedTest
    @CsvSource(value = { "AUTO_DETECT", "SINGLE", "MULTIPLE" })
    void testSupportedStructureElement(final StructureElementSupport availableSupport) {
        final PropertyValidator testee = forStructureElement(availableSupport);
        assertDoesNotThrow(() -> testee.validate(new AdapterProperties(Map.of())));
        assertDoesNotThrow(() -> testee.validate(adapterProperties("")));
        assertDoesNotThrow(() -> testee.validate(adapterProperties("abc")));
    }

    @Test
    void testValidatorChain() throws PropertyValidationException {
        final PropertyValidator v1 = mock(PropertyValidator.class);
        final PropertyValidator v2 = mock(PropertyValidator.class);
        final AdapterProperties properties = new AdapterProperties(Map.of("a", "1"));
        PropertyValidator.chain().add(v1).add(v2).validate(properties);
        verify(v1).validate(properties);
        verify(v2).validate(properties);
    }

    PropertyValidator forStructureElement(final StructureElementSupport availableSupport) {
        return PropertyValidator.forStructureElement(availableSupport, ELEMENT_NAME, PROPERTY);
    }

    AdapterProperties adapterProperties(final String value) {
        return new AdapterProperties(Map.of(PROPERTY, value));
    }
}
