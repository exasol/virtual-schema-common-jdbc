package com.exasol.adapter.dialects.rewriting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.*;

import com.exasol.ExaMetadata;
import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.dummy.DummyConnectionDefinitionBuilder;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.jdbc.BaseRemoteMetadataReader;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.properties.DataTypeDetection;
import com.exasol.adapter.sql.TestSqlStatementFactory;
import org.mockito.Mockito;

class ImportFromJDBCQueryRewriterTest extends AbstractQueryRewriterTestBase {
        private static final List<DataType> EMPTY_SELECT_LIST_DATA_TYPES = Collections.emptyList();

        @Test
        void rewriteWithJdbcConnection() throws AdapterException, SQLException {
                final AdapterProperties properties = new AdapterProperties(Map.of("CONNECTION_NAME", CONNECTION_NAME));
                final SqlDialect dialect = new DummySqlDialect(null, properties, null);
                final ExaMetadata exaMetadataMock = Mockito.mock(ExaMetadata.class);
                when(exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
                final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(mockConnection(),
                                properties, exaMetadataMock);
                final QueryRewriter queryRewriter = new ImportFromJDBCQueryRewriter(dialect, metadataReader);
                assertThat(queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(),
                                EMPTY_SELECT_LIST_DATA_TYPES, EXA_METADATA, properties),
                                equalTo("IMPORT FROM JDBC AT " + CONNECTION_NAME
                                                + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
        }

        @Test
        void rewriteWithCustomConnectionDefinitionBuilder() throws AdapterException, SQLException {
                final SqlDialect dialect = new DummySqlDialect(null, AdapterProperties.emptyProperties(), null);
                final ExaMetadata exaMetadataMock = Mockito.mock(ExaMetadata.class);
                when(exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
                final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(mockConnection(),
                                AdapterProperties.emptyProperties(), exaMetadataMock);
                final QueryRewriter queryRewriter = new ImportFromJDBCQueryRewriter(dialect, metadataReader,
                                new DummyConnectionDefinitionBuilder());
                assertThat(queryRewriter.rewrite(TestSqlStatementFactory.createSelectOneFromDual(),
                                EMPTY_SELECT_LIST_DATA_TYPES, EXA_METADATA, AdapterProperties.emptyProperties()),
                                equalTo("IMPORT FROM JDBC MY DUMMY DEFINITION BUILDER STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
        }

        @Test
        void rewriteWithFromResultSetDatatypeDetection() throws SQLException {
                final SqlDialect dialect = new DummySqlDialect(null, AdapterProperties.emptyProperties(), null);
                final ExaMetadata exaMetadataMock = Mockito.mock(ExaMetadata.class);
                when(exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
                final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(mockConnection(),
                                AdapterProperties.emptyProperties(), exaMetadataMock);
                final QueryRewriter queryRewriter = new ImportFromJDBCQueryRewriter(dialect, metadataReader,
                                new DummyConnectionDefinitionBuilder());
                final Exception exception = assertThrows(AdapterException.class, () -> queryRewriter.rewrite(
                                TestSqlStatementFactory.createSelectOneFromDual(), EMPTY_SELECT_LIST_DATA_TYPES,
                                EXA_METADATA,
                                new AdapterProperties(Map.of(DataTypeDetection.STRATEGY_PROPERTY, "FROM_RESULT_SET"))));

                assertThat(exception.getMessage(), equalTo(
                                "E-VSCJDBC-46: Property `IMPORT_DATA_TYPES` value 'FROM_RESULT_SET' is no longer supported. Please remove the `IMPORT_DATA_TYPES` property from the virtual schema so the default value 'EXASOL_CALCULATED' is used."));
        }
}