package com.exasol.adapter.dialects;

import java.util.logging.Logger;

/**
 * Abstract base class for all identifier converters.
 */
public abstract class AbstractIdentifierConverter implements IdentifierConverter {
    private static final Logger LOGGER = Logger.getLogger(AbstractIdentifierConverter.class.getName());
    protected final IdentifierCaseHandling unquotedIdentifierHandling;
    protected final IdentifierCaseHandling quotedIdentifierHandling;

    /**
     * Create a new instance of an {@link AbstractIdentifierConverter} derived class.
     *
     * @param unquotedIdentifierHandling handling for unquoted identifiers
     * @param quotedIdentifierHandling   handling for quoted identifiers
     */
    public AbstractIdentifierConverter(final IdentifierCaseHandling unquotedIdentifierHandling,
            final IdentifierCaseHandling quotedIdentifierHandling) {
        this.unquotedIdentifierHandling = unquotedIdentifierHandling;
        this.quotedIdentifierHandling = quotedIdentifierHandling;
        LOGGER.fine(
                () -> "Creating identifier converter with unqoted handling \"" + unquotedIdentifierHandling.toString()
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