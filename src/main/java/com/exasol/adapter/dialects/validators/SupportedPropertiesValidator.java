package com.exasol.adapter.dialects.validators;

import java.util.*;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.properties.PropertyValidationException;
import com.exasol.adapter.properties.PropertyValidator;
import com.exasol.errorreporting.ExaError;

/**
 * This class validates the list of supported properties and throws a validation error for unsupported properties.
 */
public class SupportedPropertiesValidator implements PropertyValidator {

    private final Set<String> supportedProperties = new HashSet<>();

    /**
     * Add additional properties to the list of supported properties.
     *
     * @param additionalProperties properties to add
     * @return this for fluent programming
     */
    public SupportedPropertiesValidator add(final Collection<String> additionalProperties) {
        this.supportedProperties.addAll(additionalProperties);
        return this;
    }

    @Override
    public void validate(final AdapterProperties properties) throws PropertyValidationException {
        for (final String property : properties.keySet()) {
            if (!this.supportedProperties.contains(property)) {
                throw new PropertyValidationException(createUnsupportedElementMessage(property));
            }
        }
    }

    /**
     * Get a set of adapter properties that the dialect supports.
     *
     * @return set of supported properties
     */
    public Set<String> getSupportedProperties() {
        return this.supportedProperties;
    }

    /**
     * Create an error message for an unsupported property.
     *
     * @param property name of the property to report as being unsupported.
     * @return error message as string
     */
    public static String createUnsupportedElementMessage(final String property) {
        return ExaError.messageBuilder("E-VSCJDBC-13")
                .message("This dialect does not support property {{property}}.", property)
                .mitigation("Please, do not set this property.") //
                .toString();
    }
}
