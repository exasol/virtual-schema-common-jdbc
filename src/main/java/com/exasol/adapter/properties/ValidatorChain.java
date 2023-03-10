package com.exasol.adapter.properties;

import java.util.*;

import com.exasol.adapter.AdapterProperties;

/**
 * Validate the specified properties by applying all property validators subsequently.
 */
public class ValidatorChain implements PropertyValidator {

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
     * Add a list of property validators to the validator chain.
     *
     * @param validators
     * @return this for fluent programming
     */
    public ValidatorChain addAll(final Collection<PropertyValidator> validators) {
        this.propertyValidators.addAll(validators);
        return this;
    }

    @Override
    public void validate(final AdapterProperties properties) throws PropertyValidationException {
        for (final PropertyValidator validator : this.propertyValidators) {
            validator.validate(properties);
        }
    }
}
