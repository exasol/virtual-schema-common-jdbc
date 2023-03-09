package com.exasol.adapter.jdbc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Wildcards {
    private static final Pattern PATTERN = Pattern.compile("[_%]");

    public static String escape(final String input) {
        final Matcher matcher = PATTERN.matcher(input);
        return matcher.find() ? matcher.replaceAll("\\\\$0") : input;
    }

    private Wildcards() {
        // only static usage
    }
}
