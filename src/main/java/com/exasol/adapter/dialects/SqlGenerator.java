package com.exasol.adapter.dialects;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.sql.SqlNode;

public interface SqlGenerator {
    public String generateSqlFor(final SqlNode sqlNode) throws AdapterException;
}
