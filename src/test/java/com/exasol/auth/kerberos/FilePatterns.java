package com.exasol.auth.kerberos;

import java.util.regex.Pattern;
import java.io.File;

/**
 * Provides OS-independent file name patterns for tests.
 */
public class FilePatterns {
    private static final String SEPARATOR_PATTERN = Pattern.quote(File.separator).toString();

    /** JAAS_CONFIG_PATTERN */
    public static final String JAAS_CONFIG_PATTERN = pattern(".*/jaas_.*\\.conf");
    /** KERBEROS_CONFIG_PATTERN */
    public static final String KERBEROS_CONFIG_PATTERN = pattern(".*/krb_.*\\.conf");

    private static String pattern(final String originalPattern) {
        if (File.separator.equals("/")) {
            return originalPattern;
        }
        return originalPattern.replace("/", SEPARATOR_PATTERN);
    }
}
