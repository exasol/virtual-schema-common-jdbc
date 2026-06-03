package com.exasol.adapter.dialects.rewriting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.exasol.*;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.jdbc.*;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.properties.DataTypeDetection;
import com.exasol.adapter.properties.DataTypeDetection.Strategy;
import com.exasol.adapter.sql.TestSqlStatementFactory;

class AbstractQueryRewriterTest {
    private static final List<DataType> EMPTY_SELECT_LIST_DATA_TYPES = Collections.emptyList();
    private static final List<DataType> SELECT_LIST_DATA_TYPES = List.of(DataType.createDecimal(18, 0));

    @Test
    void rewriteWithoutConnectionNameUsesConnectionDefinitionBuilderAndOldImportStatementOverload()
            throws AdapterException, SQLException {
        final DummyQueryRewriter testee = new DummyQueryRewriter(dummyDialect(AdapterProperties.emptyProperties()),
                new FixedConnectionDefinitionBuilder());

        assertThat(testee.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), EMPTY_SELECT_LIST_DATA_TYPES, null,
                AdapterProperties.emptyProperties()),
                equalTo("old:CONNECTION DEFINITION:SELECT 1 FROM \"DUAL\""));
    }

    @Test
    void rewriteWithConnectionNamePassesConnectionInformationToConnectionDefinitionBuilder()
            throws AdapterException, SQLException {
        final AdapterProperties properties = new AdapterProperties(Map.of("CONNECTION_NAME", "my_connection"));
        final DummyQueryRewriter testee = new DummyQueryRewriter(dummyDialect(properties),
                new ConnectionUserDefinitionBuilder());

        assertThat(testee.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), EMPTY_SELECT_LIST_DATA_TYPES,
                exaMetadata("connection_user"), properties),
                equalTo("old:USER connection_user:SELECT 1 FROM \"DUAL\""));
    }

    @Test
    void rewriteWithSelectListDataTypesUsesDefaultNewImportStatementOverload() throws AdapterException, SQLException {
        final DummyQueryRewriter testee = new DummyQueryRewriter(dummyDialect(AdapterProperties.emptyProperties()),
                new FixedConnectionDefinitionBuilder());

        assertThat(testee.rewrite(TestSqlStatementFactory.createSelectOneFromDual(), SELECT_LIST_DATA_TYPES, null,
                AdapterProperties.emptyProperties()),
                equalTo("old:CONNECTION DEFINITION:SELECT 1 FROM \"DUAL\""));
    }

    @Test
    void rewriteThrowsForUnsupportedFromResultSetDataTypeDetection() {
        final DummyQueryRewriter testee = new DummyQueryRewriter(dummyDialect(AdapterProperties.emptyProperties()),
                new FixedConnectionDefinitionBuilder());
        final DataTypeDetection dataTypeDetectionMock = Mockito.mock(DataTypeDetection.class);
        when(dataTypeDetectionMock.getStrategy()).thenReturn(Strategy.FROM_RESULT_SET);

        try (final MockedStatic<DataTypeDetection> dataTypeDetection = Mockito.mockStatic(DataTypeDetection.class)) {
            dataTypeDetection.when(DataTypeDetection::from).thenReturn(dataTypeDetectionMock);
            final AdapterException exception = assertThrows(AdapterException.class,
                    () -> testee.rewrite(TestSqlStatementFactory.createSelectOneFromDual(),
                            EMPTY_SELECT_LIST_DATA_TYPES, null, AdapterProperties.emptyProperties()));

            assertThat(exception.getMessage(), equalTo(
                    "E-VSCJDBC-46: Property `IMPORT_DATA_TYPES` value 'FROM_RESULT_SET' is no longer supported. Please remove the `IMPORT_DATA_TYPES` property from the virtual schema so the default value 'EXASOL_CALCULATED' is used."));
        }
    }

    @Test
    void getConnectionInformationReturnsNamedConnectionInformation() throws AdapterException {
        final DummyQueryRewriter testee = new DummyQueryRewriter(null, new FixedConnectionDefinitionBuilder());

        assertThat(testee.getConnectionInformation(exaMetadata("connection_user"),
                new AdapterProperties(Map.of("CONNECTION_NAME", "my_connection"))).getUser(),
                equalTo("connection_user"));
    }

    @Test
    void getConnectionInformationReturnsNullWithoutConnectionName() throws AdapterException {
        final DummyQueryRewriter testee = new DummyQueryRewriter(null, new FixedConnectionDefinitionBuilder());

        assertThat(testee.getConnectionInformation(null, AdapterProperties.emptyProperties()), equalTo(null));
    }

    @Test
    void testGetConnectionInformationThrowsException() throws ExaConnectionAccessException {
        final DummyQueryRewriter dummyQueryRewriter = new DummyQueryRewriter(null, (RemoteMetadataReader) null);
        final ExaMetadata exaMetadataMock = Mockito.mock(ExaMetadata.class);
        when(exaMetadataMock.getConnection("my_connection")).thenThrow(ExaConnectionAccessException.class);
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> dummyQueryRewriter.getConnectionInformation(exaMetadataMock,
                        new AdapterProperties(Map.of("CONNECTION_NAME", "my_connection"))));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-8"));
    }

    private static SqlDialect dummyDialect(final AdapterProperties properties) {
        return new DummySqlDialect(null, properties, null);
    }

    private static ExaMetadata exaMetadata(final String username) {
        return ExaMetadataStub.builder()
                .exaConnectionInformation(ExaConnectionInformationStub.builder()
                        .user(username)
                        .password("password")
                        .address("address")
                        .build())
                .build();
    }

    static class DummyQueryRewriter extends AbstractQueryRewriter {
        /**
         * Create a new instance of a {@link AbstractQueryRewriter}.
         *
         * @param dialect              dialect
         * @param remoteMetadataReader remote metadata reader
         */
        protected DummyQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader) {
            super(dialect, remoteMetadataReader, new BaseConnectionDefinitionBuilder());
        }

        protected DummyQueryRewriter(final SqlDialect dialect,
                final ConnectionDefinitionBuilder connectionDefinitionBuilder) {
            super(dialect, null, connectionDefinitionBuilder);
        }

        @Override
        protected String generateImportStatement(final String connectionDefinition, final String pushdownQuery) {
            return "old:" + connectionDefinition + ":" + pushdownQuery;
        }
    }

    static class FixedConnectionDefinitionBuilder implements ConnectionDefinitionBuilder {
        @Override
        public String buildConnectionDefinition(final AdapterProperties properties,
                final ExaConnectionInformation exaConnectionInformation) {
            return "CONNECTION DEFINITION";
        }
    }

    static class ConnectionUserDefinitionBuilder implements ConnectionDefinitionBuilder {
        @Override
        public String buildConnectionDefinition(final AdapterProperties properties,
                final ExaConnectionInformation exaConnectionInformation) {
            return "USER " + exaConnectionInformation.getUser();
        }
    }
}
