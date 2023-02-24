package com.exasol.adapter.properties;

import java.util.Map;

import com.exasol.adapter.AdapterProperties;

/**
 * Abstract class for validators for adapter properties
 */
public class PropertyValidator {

    /**
     * @return empty chain of property validators, enabling to add arbitrary property validators and validate a given
     *         map of properties by applying all validators subsequently.
     */
    public static ValidatorChain chain() {
        return new ValidatorChain();
    }

    /**
     * @param properties
     * @return {@code true} if one of the keys of parameter {properties} requires to refresh the virtual schema.
     */
    public static boolean requiresRefreshOfVirtualSchema(final Map<String, String> properties) {
        return properties.containsKey(TableCountLimit.MAXTABLES_PROPERTY)
                || AdapterProperties.isRefreshingVirtualSchemaRequired(properties);
    }

    private final String propertyName;
    private final PropertyValueValidator valueValidator;

    /**
     * Create a new instance of PropertyValidator
     *
     * @param propertyName   name of the property
     * @param valueValidator Validator for the value of the current property in the the property is set
     */
    public PropertyValidator(final String propertyName, final PropertyValueValidator valueValidator) {
        this.propertyName = propertyName;
        this.valueValidator = valueValidator;
    }

    /**
     * @return name of the property to validate
     */
    public String propertyName() {
        return this.propertyName;
    }

    /**
     * Validate the property
     *
     * @param properties adapter properties to validate
     * @throws PropertyValidationException in case validation fails
     */
    public void validate(final AdapterProperties properties) throws PropertyValidationException {
        if (properties.containsKey(this.propertyName)) {
            this.valueValidator.validate(properties.get(this.propertyName));
        }
    }

    /**
     * Validator for the value of the current property in the the property is set.
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
