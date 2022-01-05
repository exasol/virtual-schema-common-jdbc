package com.exasol.adapter.dialects;

import java.util.logging.Logger;

/**
 * Abstract base class for all identifier converters.
 */
public abstract class AbstractIdentifierConverter implements IdentifierConverter {
    private static final Logger LOGGER = Logger.getLogger(AbstractIdentifierConverter.class.getName());
    /** Strategy for handling unquoted identifiers. */
    protected final IdentifierCaseHandling unquotedIdentifierHandling;
    /** Strategy for handling quoted identifiers. */
    protected final IdentifierCaseHandling quotedIdentifierHandling;

    /**
     * Create a new instance of an {@link AbstractIdentifierConverter} derived class.
     *
     * @param unquotedIdentifierHandling handling for unquoted identifiers
     * @param quotedIdentifierHandling   handling for quoted identifiers
     */
    protected AbstractIdentifierConverter(final IdentifierCaseHandling unquotedIdentifierHandling,
            final IdentifierCaseHandling quotedIdentifierHandling) {
        this.unquotedIdentifierHandling = unquotedIdentifierHandling;
        this.quotedIdentifierHandling = quotedIdentifierHandling;
        LOGGER.fine(
                () -> "Creating identifier converter with unquoted handling \"" + unquotedIdentifierHandling.toString()
                        + "\" and quoted handling \"" + quotedIdentifierHandling + "\".");
    }

    @Override
    public IdentifierCaseHandling getUnquotedIdentifierHandling() {
        return this.unquotedIdentifierHandling;
    }

    @Override
    public IdentifierCaseHandling getQuotedIdentifierHandling() {
        return this.quotedIdentifierHandling;
    }
}