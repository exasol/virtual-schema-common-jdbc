package com.exasol.adapter.properties;

import com.exasol.adapter.AdapterProperties;

/**
 * Validator for property {@link AdapterProperties#SCHEMA_NAME_PROPERTY}
 */
public class SchemaNameProperty {
    /**
     * @param dialect name of the current dialect
     * @return {@link PropertyValidator} for mandatory property {@link AdapterProperties#SCHEMA_NAME_PROPERTY}.
     */
    public static PropertyValidator validator(final String dialect) {
        return MandatoryProperty.validator(dialect, "schema name", AdapterProperties.SCHEMA_NAME_PROPERTY);
    }

    private SchemaNameProperty() {
        // only static usage
    }
}
