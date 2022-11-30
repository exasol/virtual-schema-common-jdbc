package com.exasol.adapter.dialects;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.exasol.adapter.AdapterProperties;
import com.exasol.errorreporting.ExaError;

/**
 * Special Adapter Properties for JDBC-based Virtual Schemas.
 */
public class JdbcAdapterProperties {
    private JdbcAdapterProperties() {
        // only static usage
    }

    /**
     * Select method for specification of data types for {@code IMPORT} statement.
     */
    public enum DataTypeDetection {
        /** Infer data types for {@code IMPORT} statement from values of the result set */
        FROM_RESULT_SET,
        /** Let Exasol database calculate the data types for {@code IMPORT} statement based on metadata of connection */
        EXASOL_CALCULATED;

        /** Name of the adapter property to be passed to {@code CREATE VIRTUAL SCHEMA} */
        public static final String KEY = "IMPORT_DATA_TYPES";

        /**
         * Read strategy for data type detection from {@link AdapterProperties}.
         *
         * @param properties Adapter properties that contain the property for switching the data type detection strategy
         * @return strategy for data type detection
         */
        public static DataTypeDetection from(final AdapterProperties properties) {
            return valueOrDefault(properties.get(KEY), DataTypeDetection.EXASOL_CALCULATED);
        }

        static void validate(final AdapterProperties properties) throws PropertyValidationException {
            final String string = properties.get(KEY);
            if (valueOrDefault(string, null) == null) {
                throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-41")
                        .message("Invalid value {{value}} for property {{property}}.", string, KEY)
                        .mitigation("Choose one of: {{availableValues|uq}}.", EnumSet.allOf(DataTypeDetection.class).stream()
                                .map(Enum::toString).collect(Collectors.joining(", "));
            }
        }

        static DataTypeDetection valueOrDefault(final String string, final DataTypeDetection defaultValue) {
            if ((string == null) || string.isEmpty()) {
                return DataTypeDetection.EXASOL_CALCULATED;
            }
            for (final DataTypeDetection value : values()) {
                if (value.name().equals(string)) {
                    return value;
                }
            }
            return defaultValue;
        }
    }
}