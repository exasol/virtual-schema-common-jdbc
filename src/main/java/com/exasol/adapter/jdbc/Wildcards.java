package com.exasol.adapter.jdbc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Escape SQL wildcards in string to enable to request column metadata from JDBC driver with exact match for names of
 * catalog, schema, and table.
 */
public class Wildcards {
    private static final Pattern PATTERN = Pattern.compile("[_%]");

    /**
     * @param input Name of catalog, schema, or table
     * @return name with potential wildcards escaped.
     */
    public static String escape(final String input) {
        final Matcher matcher = PATTERN.matcher(input);
        return matcher.find() ? matcher.replaceAll("\\\\$0") : input;
    }

    private Wildcards() {
        // only static usage
    }
}
