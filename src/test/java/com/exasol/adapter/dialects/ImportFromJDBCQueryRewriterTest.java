package com.exasol.adapter.dialects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.ExaConnectionAccessException;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.jdbc.BaseRemoteMetadataReader;
import com.exasol.adapter.sql.TestSqlStatementFactory;

public class ImportFromJDBCQueryRewriterTest extends AbstractQueryRewriterTestBase {

    @Test
    void testRewriteWithJdbcConnection() throws AdapterException, SQLException, ExaConnectionAccessException {
        final Connection connectionMock = mockConnection();
        final AdapterProperties properties = new AdapterProperties(Map.of("CONNECTION_NAME", CONNECTION_NAME));
        final SqlDialect dialect = new DummySqlDialect(null, properties);
        final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(connectionMock, properties);

        final QueryRewriter queryRewriter = new ImportFromJDBCQueryRewriter(dialect, metadataReader);
        assertThat(queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), EXA_METADATA, properties),
                equalTo("IMPORT FROM JDBC AT " + CONNECTION_NAME + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }
}
