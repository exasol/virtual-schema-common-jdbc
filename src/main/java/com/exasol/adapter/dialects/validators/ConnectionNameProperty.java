package com.exasol.adapter.dialects.validators;

import static com.exasol.adapter.AdapterProperties.CONNECTION_NAME_PROPERTY;
import static com.exasol.adapter.AdapterProperties.DEBUG_ADDRESS_PROPERTY;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.properties.PropertyValidationException;
import com.exasol.adapter.properties.PropertyValidator;
import com.exasol.errorreporting.ExaError;

/**
 * This class enables to validate the value of the connection name property.
 */
public class ConnectionNameProperty implements PropertyValidator {
    /**
     * @return new instance of {@class PropertyValidator} for validation of debug address property.
     */
    public static PropertyValidator validator() {
        return PropertyValidator.ignoreEmpty(DEBUG_ADDRESS_PROPERTY, new DebugPortNumberProperty());
    }

    @Override
    public void validate(final AdapterProperties properties) throws PropertyValidationException {
        if (!properties.hasConnectionName()) {
            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-14")
                    .message("Please specify a connection using the property {{connectionNameProperty}}.",
                            CONNECTION_NAME_PROPERTY) //
                    .toString());
        }
    }
}