package com.exasol.adapter.jdbc;

/**
 * Container class for property constants
 */
final public class JDBCAdapterProperties {
    /**
     * Property for maximum number of tables to be mapped; exceeding this limit (default 1000) will abort virtual schema
     * creation or refresh.
     */
    public static final String JDBC_MAXTABLES_PROPERTY = "MAX_TABLE_COUNT";

    private JDBCAdapterProperties() {
        // prevent instantiation
    }
}
