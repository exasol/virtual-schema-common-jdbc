package com.exasol.auth.kerberos;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Provides OS-independent file name patterns for tests -- <3 M$.
 */
public class FilePatterns {
    private static final String SEPARATOR_PATTERN = Pattern.quote(File.separator).toString();

    private static String pattern(final String originalPattern) {
        if (File.separator.equals("/")) {
            return originalPattern;
        }
        return originalPattern.replace("/", SEPARATOR_PATTERN);
    }

    /** JAAS_CONFIG_PATTERN */
    public static final String JAAS_CONFIG_PATTERN = pattern(".*/jaas_.*\\.conf");
    /** KERBEROS_CONFIG_PATTERN */
    public static final String KERBEROS_CONFIG_PATTERN = pattern(".*/krb_.*\\.conf");

}
