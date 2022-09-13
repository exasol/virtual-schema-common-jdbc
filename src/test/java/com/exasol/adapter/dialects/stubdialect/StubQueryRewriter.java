package com.exasol.adapter.dialects.stubdialect;

import java.sql.SQLException;
import java.util.List;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.sql.SqlStatement;

/**
 * This class implements a stub query rewriter for unit testing.
 */
public class StubQueryRewriter implements QueryRewriter {
    @Override
    public String rewrite(final SqlStatement statement, final List<DataType> selectListDataTypes,
            final ExaMetadata exaMetadata, final AdapterProperties properties) throws AdapterException, SQLException {
        return "SELECT 1 FROM DUAL";
    }
}