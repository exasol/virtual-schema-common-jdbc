package com.exasol.adapter.dialects.validators;

import static com.exasol.adapter.AdapterProperties.EXCEPTION_HANDLING_PROPERTY;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialect.ExceptionHandlingMode;
import com.exasol.adapter.properties.PropertyValidationException;
import com.exasol.adapter.properties.PropertyValidator;
import com.exasol.adapter.properties.PropertyValidator.PropertyValueValidator;
import com.exasol.errorreporting.ExaError;

/**
 * This class enables to validate the value of the exception handling property.
 * 
 * @deprecated this will be removed in the next release
 */
@Deprecated(forRemoval = true)
public class ExceptionHandlingProperty implements PropertyValueValidator {

    /**
     * @return new instance of {@link PropertyValidator} for validation of exception handling property.
     */
    public static PropertyValidator validator() {
        return PropertyValidator.ignoreEmpty(EXCEPTION_HANDLING_PROPERTY, new ExceptionHandlingProperty());
    }

    @Override
    public void validate(final String exceptionHandling) throws PropertyValidationException {
        for (final SqlDialect.ExceptionHandlingMode mode : SqlDialect.ExceptionHandlingMode.values()) {
            if (mode.name().equals(exceptionHandling)) {
                return;
            }
        }
        throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-16")
                .message("Invalid value {{exceptionHandlingValue}} for property {{exceptionHandlingProperty}}.")
                .parameter("exceptionHandlingValue", exceptionHandling)
                .parameter("exceptionHandlingProperty", EXCEPTION_HANDLING_PROPERTY)
                .mitigation("Choose one of: {{availableValues|uq}}.", Arrays.stream(ExceptionHandlingMode.values())
                        .map(Enum::toString).collect(Collectors.toList()).toString())
                .toString());
    }
}
