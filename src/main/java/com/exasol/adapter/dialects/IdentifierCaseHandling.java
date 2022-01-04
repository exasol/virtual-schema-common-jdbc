package com.exasol.adapter.dialects;

/**
 * This enumeration represents how unquoted or quoted identifiers in queries or DDLs are handled.
 */
public enum IdentifierCaseHandling {
    /** Everything is interpreted as lower-case */
    INTERPRET_AS_LOWER,
    /** Everything is interpreted as upper-case */
    INTERPRET_AS_UPPER,
    /** Casing is preserved */
    INTERPRET_CASE_SENSITIVE
}