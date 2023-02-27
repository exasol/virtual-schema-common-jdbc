package com.exasol.adapter.properties;

import com.exasol.adapter.dialects.SqlDialect.StructureElementSupport;
import com.exasol.adapter.properties.PropertyValidator.PropertyValueValidator;
import com.exasol.errorreporting.ExaError;

/**
 * Validator for a property specifying a structure element, i.e. a catalog or a schema.
 */
class StructureElementSupportValidator implements PropertyValueValidator {
    private final StructureElementSupport availableSupport;
    private final String elementName;
    private final String propertyName;

    StructureElementSupportValidator(final StructureElementSupport availableSupport, final String elementName,
            final String propertyName) {
        this.availableSupport = availableSupport;
        this.elementName = elementName;
        this.propertyName = propertyName;
    }

    @Override
    public void validate(final String propertyValue) throws PropertyValidationException {
        if (this.availableSupport == StructureElementSupport.NONE) {
            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-44")
                    .message("This dialect does not support {{unsupportedElement}}.", this.elementName)
                    .mitigation("Please, do not set property {{property}}.", this.propertyName) //
                    .toString());
        }
    }
}
