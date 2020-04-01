package com.exasol.adapter.jdbc;

import static com.exasol.adapter.AdapterProperties.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.exasol.ExaMetadata;
import com.exasol.adapter.*;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.metadata.*;
import com.exasol.adapter.request.*;
import com.exasol.adapter.response.*;
import com.exasol.adapter.sql.SqlStatement;
import com.exasol.adapter.sql.TestSqlStatementFactory;

class JdbcAdapterTest {
    private static final String SCHEMA_NAME = "THE_SCHEMA";
    private static final String TEST_DIALECT_NAME = "DERBY";
    private final VirtualSchemaAdapter adapter = new JdbcAdapterFactory().createAdapter();
    private Map<String, String> rawProperties;

    @BeforeEach
    void beforeEach() {
        this.rawProperties = new HashMap<>();
    }

    @Test
    void testPushdown() throws AdapterException {
        final PushDownResponse response = pushStatementDown(TestSqlStatementFactory.createSelectOneFromSysDummy());
        assertThat(response.getPushDownSql(), equalTo("IMPORT INTO (c1 DECIMAL(10, 0))" //
                + " FROM JDBC" //
                + " AT 'jdbc:derby:memory:test;create=true;' USER '' IDENTIFIED BY ''"//
                + " STATEMENT 'SELECT 1 FROM \"SYSIBM\".\"SYSDUMMY1\"'"));
    }

    private PushDownResponse pushStatementDown(final SqlStatement statement) throws AdapterException {
        setTestSqlDialectProperty();
        setDerbyConnectionProperties();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        final List<TableMetadata> involvedTablesMetadata = null;
        final PushDownRequest request = new PushDownRequest(TEST_DIALECT_NAME, createSchemaMetadataInfo(), statement,
                involvedTablesMetadata);
        final ExaMetadata exaMetadataMock = mock(ExaMetadata.class);
        return this.adapter.pushdown(exaMetadataMock, request);
    }

    private void setTestSqlDialectProperty() {
        this.rawProperties.put(SQL_DIALECT_PROPERTY, TEST_DIALECT_NAME);
    }

    private void setDerbyConnectionProperties() {
        this.rawProperties.put(CONNECTION_STRING_PROPERTY, "jdbc:derby:memory:test;create=true;");
        this.rawProperties.put(USERNAME_PROPERTY, "");
        this.rawProperties.put(PASSWORD_PROPERTY, "");
    }

    private SchemaMetadataInfo createSchemaMetadataInfo() {
        return new SchemaMetadataInfo(SCHEMA_NAME, "", this.rawProperties);
    }

    @Test
    void testPushdownWithIllegalStatementThrowsException() {
        assertThrows(RemoteMetadataReaderException.class,
                () -> pushStatementDown(TestSqlStatementFactory.createSelectOneFromDual()));
    }

    @Test
    void testGetCapabilities() throws AdapterException {
        setTestSqlDialectProperty();
        setDerbyConnectionProperties();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        final GetCapabilitiesRequest request = new GetCapabilitiesRequest(TEST_DIALECT_NAME,
                createSchemaMetadataInfo());
        final ExaMetadata exaMetadataMock = mock(ExaMetadata.class);
        final GetCapabilitiesResponse response = this.adapter.getCapabilities(exaMetadataMock, request);
        assertAll(
                () -> assertThat(response.getCapabilities().getMainCapabilities(),
                        contains(MainCapability.ORDER_BY_EXPRESSION)),
                () -> assertThat(response.getCapabilities().getLiteralCapabilities(), contains(LiteralCapability.NULL)),
                () -> assertThat(response.getCapabilities().getAggregateFunctionCapabilities(),
                        contains(AggregateFunctionCapability.COUNT_STAR)),
                () -> assertThat(response.getCapabilities().getPredicateCapabilities(),
                        contains(PredicateCapability.AND)),
                () -> assertThat(response.getCapabilities().getScalarFunctionCapabilities(),
                        contains(ScalarFunctionCapability.ADD)));
    }

    @Test
    void testGetCapabilitiesWithExcludedCapabilitiesList() throws AdapterException {
        setTestSqlDialectProperty();
        setDerbyConnectionProperties();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        this.rawProperties.put(EXCLUDED_CAPABILITIES_PROPERTY,
                "ORDER_BY_EXPRESSION, LITERAL_NULL, FN_AGG_COUNT_STAR, FN_PRED_AND, FN_ADD");
        final GetCapabilitiesRequest request = new GetCapabilitiesRequest(TEST_DIALECT_NAME,
                createSchemaMetadataInfo());
        final ExaMetadata exaMetadataMock = mock(ExaMetadata.class);
        final GetCapabilitiesResponse response = this.adapter.getCapabilities(exaMetadataMock, request);
        assertThat(response.getCapabilities().getMainCapabilities(),
                not(contains(MainCapability.ORDER_BY_EXPRESSION, LiteralCapability.NULL,
                        AggregateFunctionCapability.COUNT_STAR, PredicateCapability.AND,
                        ScalarFunctionCapability.ADD)));
    }

    @Test
    void testDropVirtualSchemaMustSucceedEvenIfDebugAddressIsInvalid() throws AdapterException {
        setTestSqlDialectProperty();
        setDerbyConnectionProperties();
        final ExaMetadata exaMetadataMock = mock(ExaMetadata.class);
        this.rawProperties.put(AdapterProperties.DEBUG_ADDRESS_PROPERTY, "this_is_an:invalid_debug_address");
        final DropVirtualSchemaRequest dropRequest = new DropVirtualSchemaRequest(TEST_DIALECT_NAME,
                createSchemaMetadataInfo());
        final DropVirtualSchemaResponse response = this.adapter.dropVirtualSchema(exaMetadataMock, dropRequest);
        assertThat(response, notNullValue());
    }

    @Test
    void testSetPropertiesWithoutTablesFilter() throws AdapterException {
        setTestSqlDialectProperty();
        setDerbyConnectionProperties();
        final Map<String, String> newRawProperties = new HashMap<>();
        newRawProperties.put(SCHEMA_NAME_PROPERTY, "NEW SCHEMA");
        final SetPropertiesRequest request = new SetPropertiesRequest(TEST_DIALECT_NAME, createSchemaMetadataInfo(),
                newRawProperties);
        final ExaMetadata exaMetadataMock = mock(ExaMetadata.class);
        final SetPropertiesResponse response = this.adapter.setProperties(exaMetadataMock, request);
        assertThat(response.getSchemaMetadata().getTables(), emptyCollectionOf(TableMetadata.class));
    }

    @Test
    void testSetPropertiesWithTablesFilter() throws AdapterException, SQLException {
        final JdbcAdapter adapter = mock(JdbcAdapter.class);
        when(adapter.setProperties(any(), any())).thenCallRealMethod();
        when(adapter.readMetadata(any(), any(), any())).thenReturn(new SchemaMetadata("",
                Arrays.asList(new TableMetadata("T1", "", null, ""), new TableMetadata("T2", "", null, ""))));
        setTestSqlDialectProperty();
        setDerbyConnectionProperties();
        final Map<String, String> newRawProperties = new HashMap<>();
        newRawProperties.put(SCHEMA_NAME_PROPERTY, "NEW SCHEMA");
        newRawProperties.put(TABLE_FILTER_PROPERTY, "T1, T2");
        final SetPropertiesRequest request = new SetPropertiesRequest(TEST_DIALECT_NAME, createSchemaMetadataInfo(),
                newRawProperties);
        final ExaMetadata exaMetadataMock = mock(ExaMetadata.class);
        final SetPropertiesResponse response = adapter.setProperties(exaMetadataMock, request);
        final List<TableMetadata> tables = response.getSchemaMetadata().getTables();
        assertAll(() -> assertThat(tables, hasSize(2)), //
                () -> assertThat(tables.get(0).getName(), equalTo("T1")),
                () -> assertThat(tables.get(1).getName(), equalTo("T2")));
    }

    @Test
    void testCreateVirtualSchema() throws AdapterException {
        setTestSqlDialectProperty();
        setDerbyConnectionProperties();
        final CreateVirtualSchemaRequest request = new CreateVirtualSchemaRequest(TEST_DIALECT_NAME,
                createSchemaMetadataInfo());
        final ExaMetadata exaMetadataMock = mock(ExaMetadata.class);
        final CreateVirtualSchemaResponse response = this.adapter.createVirtualSchema(exaMetadataMock, request);
        assertAll(() -> assertThat(response, instanceOf(CreateVirtualSchemaResponse.class)),
                () -> assertThat(response.getSchemaMetadata(), instanceOf(SchemaMetadata.class)),
                () -> assertThat(response.getSchemaMetadata().getTables(), not(empty())),
                () -> assertThat(response.getSchemaMetadata().getAdapterNotes(),
                        equalTo("{\"catalogSeparator\":\"\",\"identifierQuoteString\":\"\\\"\","
                                + "\"storesLowerCaseIdentifiers\":false,\"storesUpperCaseIdentifiers\":true,"
                                + "\"storesMixedCaseIdentifiers\":false,\"supportsMixedCaseIdentifiers\":false,"
                                + "\"storesLowerCaseQuotedIdentifiers\":false,\"storesUpperCaseQuotedIdentifiers\":false,"
                                + "\"storesMixedCaseQuotedIdentifiers\":true,\"supportsMixedCaseQuotedIdentifiers\":true,"
                                + "\"areNullsSortedAtEnd\":false,\"areNullsSortedAtStart\":false,"
                                + "\"areNullsSortedHigh\":true,\"areNullsSortedLow\":false}")));
    }

    @Test
    void testRefreshSelectedTables() throws AdapterException {
        setTestSqlDialectProperty();
        setDerbyConnectionProperties();
        final List<String> tablesList = new ArrayList<>();
        tablesList.add("SYSDUMMY1");
        final RefreshRequest request = new RefreshRequest(TEST_DIALECT_NAME, createSchemaMetadataInfo(), tablesList);
        final ExaMetadata exaMetadataMock = mock(ExaMetadata.class);
        final RefreshResponse response = this.adapter.refresh(exaMetadataMock, request);
        assertAll(() -> assertThat(response, instanceOf(RefreshResponse.class)),
                () -> assertThat(response.getSchemaMetadata(), instanceOf(SchemaMetadata.class)),
                () -> assertThat(response.getSchemaMetadata().getTables().get(0).getName(), equalTo("SYSDUMMY1")));
    }
}