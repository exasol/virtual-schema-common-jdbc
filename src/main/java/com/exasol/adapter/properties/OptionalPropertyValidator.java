package com.exasol.adapter.properties;

import com.exasol.adapter.AdapterProperties;

class OptionalPropertyValidator implements PropertyValidator {

    private final boolean allowEmpty;
    private final String propertyName;
    private final PropertyValueValidator valueValidator;

    OptionalPropertyValidator(final boolean allowEmpty, final String propertyName,
            final PropertyValueValidator valueValidator) {
        this.allowEmpty = allowEmpty;
        this.propertyName = propertyName;
        this.valueValidator = valueValidator;
    }

    @Override
    public void validate(final AdapterProperties properties) throws PropertyValidationException {
        if (!properties.containsKey(this.propertyName)) {
            return;
        }
        final String value = properties.get(this.propertyName);
        if (value.isEmpty() && this.allowEmpty) {
            return;
        }
        this.valueValidator.validate(value);
    }
}
