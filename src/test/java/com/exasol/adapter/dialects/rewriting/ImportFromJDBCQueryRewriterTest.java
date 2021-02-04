package com.exasol.adapter.dialects.rewriting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.dummy.DummyConnectionDefinitionBuilder;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.jdbc.BaseRemoteMetadataReader;
import com.exasol.adapter.sql.TestSqlStatementFactory;

class ImportFromJDBCQueryRewriterTest extends AbstractQueryRewriterTestBase {
    @Test
    void testRewriteWithJdbcConnection() throws AdapterException, SQLException {
        final AdapterProperties properties = new AdapterProperties(Map.of("CONNECTION_NAME", CONNECTION_NAME));
        final SqlDialect dialect = new DummySqlDialect(null, properties);
        final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(mockConnection(), properties);
        final QueryRewriter queryRewriter = new ImportFromJDBCQueryRewriter(dialect, metadataReader);
        assertThat(queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), EXA_METADATA, properties),
                equalTo("IMPORT FROM JDBC AT " + CONNECTION_NAME + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }

    @Test
    void testRewriteWithCustomConnectionDefinitionBuilder() throws AdapterException, SQLException {
        final SqlDialect dialect = new DummySqlDialect(null, AdapterProperties.emptyProperties());
        final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(mockConnection(),
                AdapterProperties.emptyProperties());
        final QueryRewriter queryRewriter = new ImportFromJDBCQueryRewriter(dialect, metadataReader,
                new DummyConnectionDefinitionBuilder());
        assertThat(
                queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), EXA_METADATA,
                        AdapterProperties.emptyProperties()),
                equalTo("IMPORT FROM JDBC MY DUMMY DEFINITION BUILDER STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }
}