package com.exasol.adapter.properties;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect.StructureElementSupport;

/**
 * Abstract class for validators for adapter properties
 */
public interface PropertyValidator {

    /**
     * @return empty chain of property validators, enabling to add arbitrary property validators and validate a given
     *         map of properties by applying all validators subsequently.
     */
    public static ValidatorChain chain() {
        return new ValidatorChain();
    }

    /**
     * Create property validator for an optional property specifying a structure element.
     *
     * @param availableSupport type of support provided by the dialect
     * @param elementName      name of the structure element to be included in the message of the validation exception
     * @param propertyName     name of the property
     * @return property validator for a property specifying a structure element
     */
    public static PropertyValidator forStructureElement(final StructureElementSupport availableSupport,
            final String elementName, final String propertyName) {
        return optional(propertyName,
                new StructureElementSupportValidator(availableSupport, elementName, propertyName));
    }

    /**
     * Create property validator for an optional property allowing an empty value.
     *
     * @param propertyName   name of the property
     * @param valueValidator Validator for the value of the current property if the property is set
     * @return property validator for a property ignoring an empty value for this property
     */
    public static PropertyValidator ignoreEmpty(final String propertyName,
            final PropertyValueValidator valueValidator) {
        return new OptionalPropertyValidator(true, propertyName, valueValidator);
    }

    /**
     * Create property validator for an optional property.
     *
     * @param propertyName   name of the property
     * @param valueValidator Validator for the value of the current property if the property is set
     * @return property validator for an optional property
     */
    public static PropertyValidator optional(final String propertyName, final PropertyValueValidator valueValidator) {
        return new OptionalPropertyValidator(false, propertyName, valueValidator);
    }

    /**
     * Validate the property
     *
     * @param properties adapter properties to validate
     * @throws PropertyValidationException in case validation fails
     */
    public void validate(final AdapterProperties properties) throws PropertyValidationException;

    /**
     * Validator for the value of the current property if the property is set.
     */
    @FunctionalInterface
    public interface PropertyValueValidator {
        /**
         * @param propertyValue value of the property
         * @throws PropertyValidationException if validation fails
         */
        public void validate(String propertyValue) throws PropertyValidationException;
    }
}
