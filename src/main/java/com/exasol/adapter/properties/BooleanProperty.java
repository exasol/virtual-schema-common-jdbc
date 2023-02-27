package com.exasol.adapter.properties;

import java.util.regex.Pattern;

import com.exasol.adapter.properties.PropertyValidator.PropertyValueValidator;
import com.exasol.errorreporting.ExaError;

/**
 * This class validates boolean adapter properties.
 */
public class BooleanProperty implements PropertyValueValidator {

    /**
     * @param propertyName name of the property
     * @return new instance of {@link BooleanProperty} for validation of the specified boolean adapter property
     */
    public static PropertyValidator validator(final String propertyName) {
        return PropertyValidator.optional(propertyName, new BooleanProperty(propertyName));
    }

    private static final Pattern BOOLEAN_PROPERTY_VALUE_PATTERN = Pattern.compile("^TRUE$|^FALSE$",
            Pattern.CASE_INSENSITIVE);
    private final String propertyName;

    BooleanProperty(final String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public void validate(final String propertyValue) throws PropertyValidationException {
        if (!BOOLEAN_PROPERTY_VALUE_PATTERN.matcher(propertyValue).matches()) {
            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-15")
                    .message("The value {{value}} for property {{property}} is invalid. "
                            + "It has to be either 'true' or 'false' (case insensitive).")
                    .parameter("value", propertyValue) //
                    .parameter("property", this.propertyName).toString());
        }
    }
}
