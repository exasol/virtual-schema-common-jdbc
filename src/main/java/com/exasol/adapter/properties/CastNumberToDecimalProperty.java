package com.exasol.adapter.properties;

import java.util.regex.Pattern;

import com.exasol.adapter.properties.PropertyValidator.PropertyValueValidator;
import com.exasol.errorreporting.ExaError;

/**
 * This class validates properties that specify casting numbers to decimal.
 */
public class CastNumberToDecimalProperty implements PropertyValueValidator {

    /**
     * @param propertyName name of the property
     * @return new instance of {@link CastNumberToDecimalProperty} for validation of the specified property casting
     *         numbers to decimal
     */
    public static PropertyValidator validator(final String propertyName) {
        return PropertyValidator.optional(propertyName, new CastNumberToDecimalProperty(propertyName));
    }

    private static final Pattern PATTERN = Pattern.compile("\\s*(\\d+)\\s*,\\s*(\\d+)\\s*");
    private final String propertyName;

    CastNumberToDecimalProperty(final String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public void validate(final String propertyValue) throws PropertyValidationException {
        if (!PATTERN.matcher(propertyValue).matches()) {
            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-19")
                    .message("Unable to parse adapter property {{propertyName}} value {{value}}" //
                            + " into a number's precision and scale.", //
                            this.propertyName, propertyValue) //
                    .mitigation("Please use format '<precision>,<scale>'" //
                            + " with <precision> and <scale> being integer numbers.") //
                    .toString());
        }
    }
}
