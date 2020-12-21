package com.exasol.adapter.dialects.rewriting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.ExaConnectionAccessException;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.dialects.rewriting.ImportFromJDBCQueryRewriter;
import com.exasol.adapter.jdbc.BaseRemoteMetadataReader;
import com.exasol.adapter.sql.TestSqlStatementFactory;

class ImportFromJDBCQueryRewriterTest extends AbstractQueryRewriterTestBase {
    @Test
    void testRewriteWithJdbcConnection() throws AdapterException, SQLException, ExaConnectionAccessException {
        final AdapterProperties properties = new AdapterProperties(Map.of("CONNECTION_NAME", CONNECTION_NAME));
        final QueryRewriter queryRewriter = this.getQueryRewriter(mockConnection(), properties);
        assertThat(queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), EXA_METADATA, properties),
                equalTo("IMPORT FROM JDBC AT " + CONNECTION_NAME + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }

    private QueryRewriter getQueryRewriter(final Connection connection, final AdapterProperties properties) {
        final SqlDialect dialect = new DummySqlDialect(null, properties);
        final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(connection, properties);
        return new ImportFromJDBCQueryRewriter(dialect, metadataReader);
    }
}
