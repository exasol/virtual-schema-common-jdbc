package com.exasol.adapter.jdbc;

import com.exasol.ExaMetadata;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExasolVersion {

    public static final Logger LOGGER = Logger.getLogger(ExasolVersion.class.getName());

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:.+)?$");

    private final int major;
    private final int minor;

    private ExasolVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public static ExasolVersion parse(final ExaMetadata exaMetadata) {
        final Matcher matcher = VERSION_PATTERN.matcher(exaMetadata.getDatabaseVersion());
        if (matcher.matches()) {
            final int major = Integer.parseInt(matcher.group(1));
            final int minor = Integer.parseInt(matcher.group(2));
            return new ExasolVersion(major, minor);
        } else {
            LOGGER.warning("Could not parse Exasol version " + exaMetadata.getDatabaseVersion());
            return new ExasolVersion(0, 0);
        }
    }

    public boolean atLeast(int targetMajor, int targetMinor) {
        return this.major > targetMajor || (this.major == targetMajor && this.minor >= targetMinor);
    }
}
