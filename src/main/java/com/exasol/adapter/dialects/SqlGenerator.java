package com.exasol.adapter.dialects;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.sql.SqlNode;

/**
 * SQL generator for {@link SqlNode}.
 */
public interface SqlGenerator {
    /**
     * Generate a SQL representation of the pass {@link SqlNode}.
     *
     * @param sqlNode sqlNode
     * @return SQL representation
     * @throws AdapterException if an error occurs while generating the SQL representation.
     */
    public String generateSqlFor(final SqlNode sqlNode) throws AdapterException;
}
