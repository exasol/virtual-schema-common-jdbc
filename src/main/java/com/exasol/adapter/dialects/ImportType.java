package com.exasol.adapter.dialects;

/**
 * This enumeration specifies different types of import into Exasol.
 */
public enum ImportType {
    /** Import form JDBC connection */
    JDBC,
    /** Import from local database */
    LOCAL,
    /** Import from remote Exasol database */
    EXA,
    /** Import from an oracle database */
    ORA
}
