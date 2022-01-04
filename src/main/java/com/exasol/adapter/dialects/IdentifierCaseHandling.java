package com.exasol.adapter.dialects;

/**
 * This enumeration represents how unquoted or quoted identifiers in queries or DDLs are handled.
 */
public enum IdentifierCaseHandling {
    /** abC --> abc */
    INTERPRET_AS_LOWER,
    /** abC --> ABC */
    INTERPRET_AS_UPPER,
    /** abC --> abC */
    INTERPRET_CASE_SENSITIVE
}