package com.exasol.adapter.properties;

import static com.exasol.adapter.AdapterProperties.TABLE_FILTER_PROPERTY;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.RemoteMetadataReaderException;
import com.exasol.errorreporting.ExaError;

/**
 * Class to handle the property for limiting the maximum number of tables for mapping.
 */
public final class TableCountLimit {

    /**
     * Property for maximum number of tables to be mapped; exceeding this limit (default 1000) will abort virtual schema
     * creation or refresh.
     */
    public static final String MAXTABLES_PROPERTY = "MAX_TABLE_COUNT";
    private static final int DEFAULT_MAX_MAPPED_TABLE_LIST_SIZE = 1000;

    /**
     * @return validator for the property controlling the maximum number of tables to be mapped.
     */
    public static PropertyValidator getValidator() {
        return new PropertyValidator(MAXTABLES_PROPERTY, TableCountLimit::validatePropertyValue);
    }

    private static void validatePropertyValue(final String value) throws PropertyValidationException {
        try {
            final int parsed = Integer.parseUnsignedInt(value);
            if (parsed == 0) {
                throw new IllegalArgumentException();
            }
        } catch (final IllegalArgumentException exception) {
            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-43") //
                    .message("Invalid parameter value.") //
                    .mitigation("The adapter property {{max_tables_property}}" //
                            + " if present, must be a positive integer.") //
                    .parameter("max_tables_property", MAXTABLES_PROPERTY) //
                    .toString());
        }
    }

    /**
     * @param properties Adapter Properties passed to {@code CREATE VIRTUAL SCHEMA}
     * @return new instance of {@link TableCountLimit} based on the properties
     */
    public static TableCountLimit from(final AdapterProperties properties) {
        if (properties.containsKey(MAXTABLES_PROPERTY)) {
            final int value = Integer.parseUnsignedInt(properties.get(MAXTABLES_PROPERTY));
            return new TableCountLimit(value);
        }
        return new TableCountLimit(DEFAULT_MAX_MAPPED_TABLE_LIST_SIZE);
    }

    private final int maxNumberOfTables;

    /**
     * @param maxNumberOfTables maximum number of tables to be accepted
     */
    public TableCountLimit(final int maxNumberOfTables) {
        this.maxNumberOfTables = maxNumberOfTables;
    }

    /**
     * @param numberOfTables actual number of tables
     * @return {@code true} if the provided actual number of tables is accepted.
     */
    public boolean accepts(final int numberOfTables) {
        return numberOfTables <= this.maxNumberOfTables;
    }

    /**
     * Verify that the given number of mapped tables does not exceed the configured max table limit.
     *
     * @param numberOfTables actual size of mapped tables
     * @throws RemoteMetadataReaderException if the table limit has been exceeded
     */
    public void validateNumberOfTables(final int numberOfTables) throws RemoteMetadataReaderException {
        if (numberOfTables > this.maxNumberOfTables) {
            throw new RemoteMetadataReaderException(ExaError.messageBuilder("E-VSCJDBC-42")
                    .message("The size of the list of the selected tables exceeds" //
                            + " the configured allowed maximum of {{current_limit}}.")
                    .mitigation(
                            "Please use the {{table_filter_property}} property to define the list of tables you need" //
                                    + " or increase the limit using the {{max_tables_property}} property.") //
                    .parameter("current_limit", this.maxNumberOfTables) //
                    .parameter("table_filter_property", TABLE_FILTER_PROPERTY) //
                    .parameter("max_tables_property", MAXTABLES_PROPERTY) //
                    .toString());
        }
    }
}
