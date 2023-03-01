package com.exasol.adapter.properties;

import java.util.ArrayList;
import java.util.List;

import com.exasol.adapter.AdapterProperties;

/**
 * Validate the specified properties by applying all property validators subsequently.
 */
public class ValidatorChain {

    private final List<PropertyValidator> propertyValidators = new ArrayList<>();

    /**
     * Add a single property validator to the validator chain.
     *
     * @param validator property validator to add
     * @return this for fluent programming
     */
    public ValidatorChain add(final PropertyValidator validator) {
        this.propertyValidators.add(validator);
        return this;
    }

    /**
     * Validate the specified properties by applying all property validators subsequently.
     *
     * @param properties adapter properties to be verified
     * @throws PropertyValidationException in case one of the validators fails to validate its corresponding property
     */
    public void validate(final AdapterProperties properties) throws PropertyValidationException {
        for (final PropertyValidator validator : this.propertyValidators) {
            validator.validate(properties);
        }
    }
}
