package com.exasol.adapter.dialects.rewriting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import com.exasol.ExaMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.dummy.DummyConnectionDefinitionBuilder;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.jdbc.BaseRemoteMetadataReader;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.sql.TestSqlStatementFactory;

@ExtendWith(MockitoExtension.class)
class ImportIntoTemporaryTableQueryRewriterTest extends AbstractQueryRewriterTestBase {
    private static final List<DataType> EMPTY_SELECT_LIST_DATA_TYPES = Collections.emptyList();

    @Mock
    private ConnectionFactory connectionFactoryMock;
    @Mock
    private ExaMetadata exaMetadataMock;

    private Connection connectionMock;

    @BeforeEach
    void beforeEach() throws SQLException {
        this.connectionMock = mockConnection();
        when(this.connectionFactoryMock.getConnection()).thenReturn(this.connectionMock);
        when(this.exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
    }

    @Test
    void testRewriteWithJdbcConnection() throws AdapterException, SQLException {
        final AdapterProperties properties = new AdapterProperties(Map.of("CONNECTION_NAME", CONNECTION_NAME));
        final SqlDialect dialect = new DummySqlDialect(this.connectionFactoryMock, properties, exaMetadataMock);
        final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(this.connectionMock, properties,
                exaMetadataMock);
        final QueryRewriter queryRewriter = new ImportIntoTemporaryTableQueryRewriter(dialect, metadataReader,
                this.connectionFactoryMock);
        assertThat(queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), //
                EMPTY_SELECT_LIST_DATA_TYPES, EXA_METADATA, properties),
                equalTo("IMPORT INTO (c1 DECIMAL(18, 0)) FROM JDBC AT " + CONNECTION_NAME
                        + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }

    @Test
    void testRewriteWithCustomConnectionDefinitionBuilder() throws AdapterException, SQLException {
        final SqlDialect dialect = new DummySqlDialect(this.connectionFactoryMock, AdapterProperties.emptyProperties(),
                exaMetadataMock);
        final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(this.connectionMock,
                AdapterProperties.emptyProperties(), exaMetadataMock);
        final QueryRewriter queryRewriter = new ImportIntoTemporaryTableQueryRewriter(dialect, metadataReader,
                this.connectionFactoryMock, new DummyConnectionDefinitionBuilder());
        assertThat(queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), //
                EMPTY_SELECT_LIST_DATA_TYPES, EXA_METADATA, AdapterProperties.emptyProperties()),
                equalTo("IMPORT INTO (c1 DECIMAL(18, 0)) FROM JDBC MY DUMMY DEFINITION BUILDER"
                        + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }
}