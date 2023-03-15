package com.exasol.adapter.jdbc;

import java.sql.DatabaseMetaData;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Escape SQL wild cards in string to enable to request column metadata from JDBC driver with exact match for names of
 * catalog, schema, and table.
 */
public class WildcardEscaper {

    private static final Pattern REGEX_WILDCARDS = Pattern.compile("[\\\\$]");
    private static final Pattern SQL_WINDCARDS = Pattern.compile("[_%]");

    /**
     * Create a new instance of the {@link WildcardEscaper}.
     * <p>
     * Use {@link DatabaseMetaData#getSearchStringEscape()} to get the escape string for wild card characters.
     *
     * @param searchStringEscape string that should be used to escape SQL wild cards
     * @return new instance of the {@link WildcardEscaper}
     */
    public static WildcardEscaper instance(final String searchStringEscape) {
        final String escaped = new WildcardEscaper(REGEX_WILDCARDS, "\\\\").escape(searchStringEscape);
        return new WildcardEscaper(SQL_WINDCARDS, escaped);
    }

    private final Pattern pattern;
    private final String replacement;

    WildcardEscaper(final Pattern pattern, final String replacement) {
        this.pattern = pattern;
        this.replacement = replacement + "$0";
    }

    /**
     * @param input Name of catalog, schema, or table
     * @return name with potential wild cards escaped.
     */
    public String escape(final String input) {
        final Matcher matcher = this.pattern.matcher(input);
        return matcher.find() ? matcher.replaceAll(this.replacement) : input;
    }
}
