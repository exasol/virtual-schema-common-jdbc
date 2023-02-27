package com.exasol.adapter.dialects.validators;

import static com.exasol.adapter.AdapterProperties.DEBUG_ADDRESS_PROPERTY;

import java.util.logging.Logger;

import com.exasol.adapter.properties.PropertyValidationException;
import com.exasol.adapter.properties.PropertyValidator;
import com.exasol.adapter.properties.PropertyValidator.PropertyValueValidator;
import com.exasol.errorreporting.ExaError;

/**
 * This class enables to validate the value of the debug address property.
 */
public class DebugPortNumberProperty implements PropertyValueValidator {
    private static final Logger LOGGER = Logger.getLogger(DebugPortNumberProperty.class.getName());

    /**
     * @return new instance of {@class PropertyValidator} for validation of debug address property.
     */
    public static PropertyValidator validator() {
        return PropertyValidator.ignoreEmpty(DEBUG_ADDRESS_PROPERTY, new DebugPortNumberProperty());
    }

    /*
     * Note that this method intentionally does not throw a validation exception but rather creates log warnings. This
     * allows dropping a schema even if the debug output port is misconfigured. Logging falls back to local logging in
     * this case.
     */
    @Override
    public void validate(final String debugAddress) throws PropertyValidationException {
        final int colonLocation = debugAddress.lastIndexOf(':');
        if (colonLocation > 0) {
            final String portAsString = debugAddress.substring(colonLocation + 1);
            try {
                final int port = Integer.parseInt(portAsString);
                if ((port < 1) || (port > 65535)) {
                    LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-40")
                            .message("Debug output port {{port|uq}} is out of range.", port) //
                            .mitigation("Port specified in property {{debugAddressProperty}} must have "
                                    + "the following format: <host>[:<port>], and be between 1 and 65535.")
                            .parameter("debugAddressProperty", DEBUG_ADDRESS_PROPERTY).toString());
                }
            } catch (final NumberFormatException ex) {
                LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-39").message(
                        "Illegal debug output port {{portAsString}}. Property {{debugAddressProperty}} must have "
                                + "the following format: <host>[:<port>], where port is a number between 1 and 65535.")
                        .parameter("debugAddressProperty", DEBUG_ADDRESS_PROPERTY)
                        .parameter("portAsString", portAsString).toString());
            }
        }
    }
}