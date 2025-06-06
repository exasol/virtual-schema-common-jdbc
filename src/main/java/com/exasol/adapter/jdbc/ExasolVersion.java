package com.exasol.adapter.jdbc;

import com.exasol.ExaMetadata;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the version of a Exasol database.
 */
public final class ExasolVersion {
    private static final Logger LOGGER = Logger.getLogger(ExasolVersion.class.getName());

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)");

    private final int major;
    private final int minor;

    private ExasolVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Factory method to create an instance based on the Exasol database metadata.
     * @param exaMetadata Metadata of the Exasol database
     * @return instance representing the Exasol version.
     */
    public static ExasolVersion parse(final ExaMetadata exaMetadata) {
        final Matcher matcher = VERSION_PATTERN.matcher(exaMetadata.getDatabaseVersion());
        if (matcher.find()) {
            final int major = Integer.parseInt(matcher.group(1));
            final int minor = Integer.parseInt(matcher.group(2));
            return new ExasolVersion(major, minor);
        } else {
            LOGGER.warning("Could not parse Exasol version '" + exaMetadata.getDatabaseVersion() + "'");
            return new ExasolVersion(0, 0);
        }
    }

    /**
     * Returns if the Exasol versions is at least the specified version.
     * @param major required major version.
     * @param minor required minor version.
     * @return true if the version is at least the specified version, false otherwise.
     */
    public boolean atLeast(int major, int minor) {
        return this.major > major || (this.major == major && this.minor >= minor);
    }
}
