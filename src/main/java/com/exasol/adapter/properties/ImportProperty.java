package com.exasol.adapter.properties;

import com.exasol.adapter.AdapterProperties;
import com.exasol.errorreporting.ExaError;

/**
 * This class validates the consistency of adapter properties for import and connection.
 */
public class ImportProperty implements PropertyValidator {

    /**
     * @param importFromProperty name of the property for import
     * @param connectionProperty name of the property for the database connection
     * @return new instance of {@link ImportProperty} for validation of the specified adapter properties
     */
    public static PropertyValidator validator(final String importFromProperty, final String connectionProperty) {
        return new ImportProperty(importFromProperty, connectionProperty);
    }

    private final String importFromProperty;
    private final String connectionProperty;

    ImportProperty(final String importFromProperty, final String connectionProperty) {
        this.importFromProperty = importFromProperty;
        this.connectionProperty = connectionProperty;
    }

    @Override
    public void validate(final AdapterProperties properties) throws PropertyValidationException {
        final boolean isDirectImport = properties.isEnabled(this.importFromProperty);
        final String value = properties.get(this.connectionProperty);
        final boolean connectionIsEmpty = ((value == null) || value.isEmpty());
        if (isDirectImport) {
            if (connectionIsEmpty) {
                throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-17")
                        .message("You defined the property {{importFromProperty}}.", this.importFromProperty)
                        .mitigation("Please also define {{connectionProperty}}.", this.connectionProperty) //
                        .toString());
            }
        } else {
            if (!connectionIsEmpty) {
                throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-18")
                        .message("You defined the property {{connectionProperty}}" //
                                + " without setting {{importFromProperty}} to 'TRUE'." //
                                + " This is not allowed.", //
                                this.connectionProperty, this.importFromProperty) //
                        .toString());
            }
        }
    }
}
