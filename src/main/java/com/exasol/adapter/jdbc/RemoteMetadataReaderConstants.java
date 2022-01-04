package com.exasol.adapter.jdbc;

import java.util.Collections;
import java.util.Set;

/**
 * This class contains constants that are relevant for any of the classes involved with reading remote metadata.
 */
public final class RemoteMetadataReaderConstants {
    private RemoteMetadataReaderConstants() {
        // prevent instantiation
    }

    /** Filter that matches any catalog */
    public static final String ANY_CATALOG = null;
    /** Filter that matches any schema */
    public static final String ANY_SCHEMA = null;
    /** Filter that matches any table */
    public static final String ANY_TABLE = "%";
    /** Filter that matches any column */
    public static final String ANY_COLUMN = "%";
    /** Constant for {@code yes} */
    public static final String JDBC_TRUE = "yes";
    /** Constant for {@code no} */
    public static final String JDBC_FALSE = "no";
    /** Default list for supported tables */
    public static final Set<String> DEFAULT_SUPPORTED_TABLE_TYPES = Set.of("TABLE", "VIEW", "SYSTEM TABLE");
    /** Any table type */
    public static final Set<String> ANY_TABLE_TYPE = Collections.emptySet();
}