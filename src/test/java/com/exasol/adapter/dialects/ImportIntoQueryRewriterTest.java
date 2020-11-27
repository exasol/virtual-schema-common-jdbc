package com.exasol.adapter.dialects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.ExaConnectionAccessException;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.jdbc.BaseRemoteMetadataReader;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.sql.TestSqlStatementFactory;

@ExtendWith(MockitoExtension.class)
class ImportIntoQueryRewriterTest extends AbstractQueryRewriterTestBase {
    @Test
    void testRewriteWithJdbcConnection(@Mock final ConnectionFactory connectionFactoryMock)
            throws AdapterException, SQLException, ExaConnectionAccessException {
        final Connection connectionMock = mockConnection();
        when(connectionFactoryMock.getConnection()).thenReturn(connectionMock);
        final AdapterProperties properties = new AdapterProperties(Map.of("CONNECTION_NAME", CONNECTION_NAME));
        final QueryRewriter queryRewriter = this.getQueryRewriter(connectionFactoryMock, connectionMock, properties);
        assertThat(queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), EXA_METADATA, properties),
                equalTo("IMPORT INTO (c1 DECIMAL(18, 0)) FROM JDBC AT " + CONNECTION_NAME
                        + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }

    private QueryRewriter getQueryRewriter(final ConnectionFactory connectionFactory, final Connection connection,
            final AdapterProperties properties) {
        final SqlDialect dialect = new DummySqlDialect(connectionFactory, properties);
        final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(connection, properties);
        return new ImportIntoQueryRewriter(dialect, metadataReader, connectionFactory);
    }
}
