package com.exasol.adapter.properties;

import com.exasol.errorreporting.ExaError;

/**
 * This class provides the message for unsupported elements in the adapter properties and can create an appropriate
 * {@link PropertyValidationException}.
 */
public class UnsupportedElement {
    /**
     * @param unsupportedElement name of unsupported element
     * @param property           name of the property
     * @return {@link PropertyValidationException}
     */
    public static PropertyValidationException validationException(final String unsupportedElement,
            final String property) {
        return new PropertyValidationException(message(unsupportedElement, property));
    }

    /**
     *
     * @param unsupportedElement name of unsupported element
     * @param property           name of the property
     * @return error message for this unsupported elements
     */
    public static String message(final String unsupportedElement, final String property) {
        return ExaError.messageBuilder("E-VSCJDBC-13")
                .message("This dialect does not support {{unsupportedElement}} property.", unsupportedElement)
                .mitigation(" Please, do not set the {{property}} property.", property) //
                .toString();
    }
}