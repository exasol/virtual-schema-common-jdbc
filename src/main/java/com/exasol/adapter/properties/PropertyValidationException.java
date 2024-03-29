package com.exasol.adapter.properties;

import com.exasol.adapter.AdapterException;

/**
 * This class represents exceptional conditions that occur during validation of the dialect's properties.
 */
public class PropertyValidationException extends AdapterException {
    /** Serialization ID */
    public static final long serialVersionUID = 1659958371335354081L;

    /**
     * Create a new {@link PropertyValidationException}.
     *
     * @param message error message
     */
    public PropertyValidationException(final String message) {
        super(message);
    }
}
