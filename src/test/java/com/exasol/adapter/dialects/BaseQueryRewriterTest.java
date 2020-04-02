package com.exasol.adapter.dialects;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.ExaConnectionAccessException;
import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.jdbc.BaseRemoteMetadataReader;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.sql.TestSqlStatementFactory;

@ExtendWith(MockitoExtension.class)
class BaseQueryRewriterTest extends AbstractQueryRewriterTestBase {
    @Test
    void testRewriteWithJdbcConnection(@Mock final ConnectionFactory connectionFactoryMock,
            @Mock final ExaMetadata exaMetadataMock)
            throws AdapterException, SQLException, ExaConnectionAccessException {
        final Connection connectionMock = mockConnection();
        final AdapterProperties properties = new AdapterProperties(Map.of("CONNECTION_NAME", CONNECTION_NAME));
        when(connectionFactoryMock.getConnection()).thenReturn(connectionMock);
        final SqlDialect dialect = new DummySqlDialect(connectionFactoryMock, properties);
        final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(connectionMock, properties);
        final QueryRewriter queryRewriter = new BaseQueryRewriter(dialect, metadataReader, connectionFactoryMock);
        assertThat(queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), EXA_METADATA, properties),
                equalTo("IMPORT INTO (c1 DECIMAL(18, 0)) FROM JDBC AT " + CONNECTION_NAME
                        + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }
}