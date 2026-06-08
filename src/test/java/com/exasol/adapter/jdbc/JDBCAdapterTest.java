package com.exasol.adapter.jdbc;

import static com.exasol.adapter.AdapterProperties.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.*;

import org.hamcrest.Matchers;
import org.itsallcode.matcher.auto.AutoMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.*;
import com.exasol.adapter.*;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.metadata.*;
import com.exasol.adapter.properties.PropertyValidationException;
import com.exasol.adapter.properties.TableCountLimit;
import com.exasol.adapter.request.*;
import com.exasol.adapter.response.*;
import com.exasol.adapter.sql.SqlStatement;
import com.exasol.adapter.sql.TestSqlStatementFactory;
import com.exasol.telemetry.TelemetryClient;

@ExtendWith(MockitoExtension.class)
class JDBCAdapterTest {

    private static final List<DataType> EMPTY_SELECT_LIST_DATA_TYPES = Collections.emptyList();
    private static final String SCHEMA_NAME = "THE_SCHEMA";
    private static final ExaConnectionInformation EXA_CONNECTION_INFORMATION = ExaConnectionInformationStub
            .builder() //
            .user("") //
            .password("") //
            .address("jdbc:derby:memory:test;create=true;") //
            .build();

    private VirtualSchemaAdapter derbyAdapter;
    private Map<String, String> rawProperties;

    @Mock
    ExaMetadata exaMetadataMock;
    @Mock
    TelemetryClient telemetryClientMock;
    @Mock
    SqlDialectFactory dialectFactoryMock;
    @Mock
    SqlDialect dialectMock;

    @BeforeEach
    void beforeEach() {
        this.rawProperties = new HashMap<>();
        @SuppressWarnings("resource") // No need to close the factory in tests
        final JDBCAdapterFactory jdbcAdapterFactory = new JDBCAdapterFactory();
        derbyAdapter = jdbcAdapterFactory.createAdapter(new AdapterContext(telemetryClientMock));
    }

    @Test
    void testPushdown() throws AdapterException, ExaConnectionAccessException {
        final PushDownResponse response = pushStatementDown(
                TestSqlStatementFactory.createSelectOneFromSysDummy(), EMPTY_SELECT_LIST_DATA_TYPES);
        assertThat(response.getPushDownSql(), equalTo("IMPORT INTO (c1 DECIMAL(10, 0))" //
                + " FROM JDBC" //
                + " AT DERBY_CONNECTION"//
                + " STATEMENT 'SELECT 1 FROM \"SYSIBM\".\"SYSDUMMY1\"'"));
    }

    @Test
    void pushdownWithSelectListDataTypes() throws AdapterException, ExaConnectionAccessException {
        final List<DataType> dataTypes = List.of(DataType.createIntervalDaySecond(1, 2),
                DataType.createGeometry(12));
        final PushDownResponse response = pushStatementDown(
                TestSqlStatementFactory.createSelectOneFromSysDummy(), dataTypes);
        assertThat(response.getPushDownSql(),
                equalTo("IMPORT INTO (c1 INTERVAL DAY (1) TO SECOND (2), c2 GEOMETRY(12))" //
                        + " FROM JDBC" //
                        + " AT DERBY_CONNECTION"//
                        + " STATEMENT 'SELECT 1 FROM \"SYSIBM\".\"SYSDUMMY1\"'"));
    }

    private PushDownResponse pushStatementDown(final SqlStatement statement,
            final List<DataType> selectListDataTypes)
            throws AdapterException, ExaConnectionAccessException {
        setDerbyConnectionNameProperty();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        final List<TableMetadata> involvedTablesMetadata = null;
        final PushDownRequest request = new PushDownRequest(createSchemaMetadataInfo(), statement,
                involvedTablesMetadata, selectListDataTypes);
        when(exaMetadataMock.getConnection("DERBY_CONNECTION")).thenReturn(EXA_CONNECTION_INFORMATION);
        when(exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
        return this.derbyAdapter.pushdown(exaMetadataMock, request);
    }

    private void setDerbyConnectionNameProperty() {
        this.rawProperties.put(CONNECTION_NAME_PROPERTY, "DERBY_CONNECTION");
    }

    private SchemaMetadataInfo createSchemaMetadataInfo() {
        return new SchemaMetadataInfo(SCHEMA_NAME, "", this.rawProperties);
    }

    @Test
    void testPushdownWithIllegalStatementThrowsException() {
        final SqlStatement statement = TestSqlStatementFactory.createSelectOneFromDual();
        final RemoteMetadataReaderException exception = assertThrows(RemoteMetadataReaderException.class,
                () -> pushStatementDown(statement, EMPTY_SELECT_LIST_DATA_TYPES));
        assertAll(() -> assertThat(exception.getMessage(), equalTo("E-VSCJDBC-30: Unable to read remote metadata"
                + " for push-down query trying to generate result column description. Please, make sure that you"
                + " provided valid CATALOG_NAME and SCHEMA_NAME properties if required. Caused by: 'Table/View"
                + " 'SYSIBM.DUAL' does not exist.'")));
    }

    @Test
    void testGetCapabilities() throws AdapterException {
        setDerbyConnectionNameProperty();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        final GetCapabilitiesRequest request = new GetCapabilitiesRequest(createSchemaMetadataInfo());
        final GetCapabilitiesResponse response = this.derbyAdapter.getCapabilities(exaMetadataMock, request);

        final Capabilities expectedCapabilities = Capabilities.builder()
                .addMain(MainCapability.ORDER_BY_EXPRESSION)
                .addLiteral(LiteralCapability.NULL)
                .addAggregateFunction(AggregateFunctionCapability.COUNT_STAR)
                .addPredicate(PredicateCapability.AND)
                .addScalarFunction(ScalarFunctionCapability.ADD)
                .build();

        assertAll(() -> assertThat(response.getCapabilities().getMainCapabilities(),
                contains(MainCapability.ORDER_BY_EXPRESSION)),
                () -> assertThat(response.getCapabilities().getLiteralCapabilities(),
                        contains(LiteralCapability.NULL)),
                () -> assertThat(response.getCapabilities().getAggregateFunctionCapabilities(),
                        contains(AggregateFunctionCapability.COUNT_STAR)),
                () -> assertThat(response.getCapabilities().getPredicateCapabilities(),
                        contains(PredicateCapability.AND)),
                () -> assertThat(response.getCapabilities().getScalarFunctionCapabilities(),
                        contains(ScalarFunctionCapability.ADD)),
                () -> assertThat(response.getCapabilities(),
                        AutoMatcher.equalTo(expectedCapabilities)));
    }

    @Test
    void testGetCapabilitiesWithExcludedCapabilitiesList() throws AdapterException {
        setDerbyConnectionNameProperty();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        this.rawProperties.put(EXCLUDED_CAPABILITIES_PROPERTY,
                "ORDER_BY_EXPRESSION, LITERAL_NULL, FN_AGG_COUNT_STAR, FN_PRED_AND, FN_ADD");
        final GetCapabilitiesRequest request = new GetCapabilitiesRequest(createSchemaMetadataInfo());
        final GetCapabilitiesResponse response = this.derbyAdapter.getCapabilities(exaMetadataMock, request);

        assertAll(() -> assertThat(response.getCapabilities().getMainCapabilities(),
                not(contains(MainCapability.ORDER_BY_EXPRESSION))),
                () -> assertThat(response.getCapabilities().getLiteralCapabilities(),
                        not(contains(LiteralCapability.NULL))),
                () -> assertThat(response.getCapabilities().getAggregateFunctionCapabilities(),
                        not(contains(AggregateFunctionCapability.COUNT_STAR))),
                () -> assertThat(response.getCapabilities().getPredicateCapabilities(),
                        not(contains(PredicateCapability.AND))),
                () -> assertThat(response.getCapabilities().getScalarFunctionCapabilities(),
                        not(contains(ScalarFunctionCapability.ADD))),
                () -> assertThat(response.getCapabilities(),
                        AutoMatcher.equalTo(Capabilities.builder().build())));
    }

    @Test
    void testGetCapabilitiesWithInvalidExcludedCapabilityMessage() {
        setDerbyConnectionNameProperty();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        this.rawProperties.put(EXCLUDED_CAPABILITIES_PROPERTY, "INVALID_MAIN_CAPABILITY");
        final GetCapabilitiesRequest request = new GetCapabilitiesRequest(createSchemaMetadataInfo());
        final JDBCAdapter jdbcAdapter = createAdapterWithMockDialect();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> jdbcAdapter.getCapabilities(exaMetadataMock, request));

        assertThat(exception.getMessage(), Matchers.startsWith("E-VSCJDBC-51: Unsupported capability 'INVALID_MAIN_CAPABILITY' for main capability."
                + " Use one of the available capabilities: "));
    }

    @Test
    void testDropVirtualSchemaMustSucceedEvenIfDebugAddressIsInvalid() throws AdapterException {
        setDerbyConnectionNameProperty();
        this.rawProperties.put(AdapterProperties.DEBUG_ADDRESS_PROPERTY, "this_is_an:invalid_debug_address");
        final DropVirtualSchemaRequest dropRequest = new DropVirtualSchemaRequest(createSchemaMetadataInfo());
        final DropVirtualSchemaResponse response = this.derbyAdapter.dropVirtualSchema(exaMetadataMock, dropRequest);
        assertThat(response, notNullValue());
    }

    @Test
    void testSetPropertiesWithoutTablesFilter() throws AdapterException, ExaConnectionAccessException {
        setDerbyConnectionNameProperty();
        final Map<String, String> newRawProperties = new HashMap<>();
        newRawProperties.put(SCHEMA_NAME_PROPERTY, "NEW SCHEMA");
        final SetPropertiesRequest request = new SetPropertiesRequest(createSchemaMetadataInfo(),
                newRawProperties);
        when(exaMetadataMock.getConnection("DERBY_CONNECTION")).thenReturn(EXA_CONNECTION_INFORMATION);
        when(exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
        final SetPropertiesResponse response = this.derbyAdapter.setProperties(exaMetadataMock, request);
        assertThat(response.getSchemaMetadata().getTables(), emptyCollectionOf(TableMetadata.class));
    }

    @Test
    void testSetPropertiesWithTablesFilter() throws AdapterException {
        when(dialectMock.readSchemaMetadata(any())).thenReturn(new SchemaMetadata("", Arrays
                .asList(new TableMetadata("T1", "", null, ""), new TableMetadata("T2", "", null, ""))));
        final JDBCAdapter jdbcAdapter = createAdapterWithMockDialect();
        setDerbyConnectionNameProperty();
        final Map<String, String> newRawProperties = new HashMap<>();
        newRawProperties.put(SCHEMA_NAME_PROPERTY, "NEW SCHEMA");
        newRawProperties.put(TABLE_FILTER_PROPERTY, "T1, T2");
        final SetPropertiesRequest request = new SetPropertiesRequest(createSchemaMetadataInfo(),
                newRawProperties);
        final SetPropertiesResponse response = jdbcAdapter.setProperties(exaMetadataMock, request);
        final List<TableMetadata> tables = response.getSchemaMetadata().getTables();
        assertAll(() -> assertThat(tables, hasSize(2)),
                () -> assertThat(tables.get(0).getName(), equalTo("T1")),
                () -> assertThat(tables.get(1).getName(), equalTo("T2")));
    }

    @Test
    void setPropertiesValidatesMergedPropertiesEvenIfChangeDoesNotRequireRefresh() throws AdapterException {
        final JDBCAdapter jdbcAdapter = createAdapterWithMockDialect();
        final SetPropertiesRequest request = new SetPropertiesRequest(createSchemaMetadataInfo(), Map.of("property", "invalid"));
        doThrow(new PropertyValidationException("mock validation error")).when(dialectMock).validateProperties();

        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                () -> jdbcAdapter.setProperties(exaMetadataMock, request));

        final ArgumentCaptor<JDBCAdapterContext> contextCaptor = ArgumentCaptor.forClass(JDBCAdapterContext.class);
        verify(dialectFactoryMock).createSqlDialect(contextCaptor.capture());
        assertAll(() -> assertThat(exception.getMessage(), equalTo("mock validation error")),
                () -> verify(dialectMock, never()).readSchemaMetadata(anyList()));
    }

    @Test
    void testCreateVirtualSchema() throws AdapterException, ExaConnectionAccessException {
        setDerbyConnectionNameProperty();
        final CreateVirtualSchemaRequest request = new CreateVirtualSchemaRequest(createSchemaMetadataInfo());
        when(exaMetadataMock.getConnection("DERBY_CONNECTION")).thenReturn(EXA_CONNECTION_INFORMATION);
        when(exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
        final CreateVirtualSchemaResponse response = this.derbyAdapter.createVirtualSchema(exaMetadataMock, request);
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
    void testRefreshSelectedTables() throws AdapterException, ExaConnectionAccessException {
        setDerbyConnectionNameProperty();
        final List<String> tablesList = new ArrayList<>();
        tablesList.add("SYSDUMMY1");
        final RefreshRequest request = new RefreshRequest(createSchemaMetadataInfo(), tablesList);
        when(exaMetadataMock.getConnection("DERBY_CONNECTION")).thenReturn(EXA_CONNECTION_INFORMATION);
        when(exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
        final RefreshResponse response = this.derbyAdapter.refresh(exaMetadataMock, request);
        assertAll(() -> assertThat(response, instanceOf(RefreshResponse.class)),
                () -> assertThat(response.getSchemaMetadata(), instanceOf(SchemaMetadata.class)),
                () -> assertThat(response.getSchemaMetadata().getTables().get(0).getName(),
                        equalTo("SYSDUMMY1")));
    }

    @Test
    void refreshValidatesProperties() throws AdapterException {
        final JDBCAdapter jdbcAdapter = createAdapterWithMockDialect();
        final RefreshRequest request = new RefreshRequest(createSchemaMetadataInfo(), List.of("SYSDUMMY1"));
        when(dialectMock.readSchemaMetadata(anyList())).thenReturn(new SchemaMetadata("", List.of()));

        jdbcAdapter.refresh(exaMetadataMock, request);

        verify(dialectMock).validateProperties();
    }

    @ParameterizedTest
    @ValueSource(strings = { "hello", "0", "-1", "", "1,700" })
    void testValidateMaxTablesAtCreate(final String paramValue) {
        setDerbyConnectionNameProperty();
        this.rawProperties.put(TableCountLimit.MAXTABLES_PROPERTY, paramValue);
        final SchemaMetadataInfo schemaMetadataInfo = createSchemaMetadataInfo();
        final CreateVirtualSchemaRequest request = new CreateVirtualSchemaRequest(schemaMetadataInfo);
        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                () -> this.derbyAdapter.createVirtualSchema(null, request));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-43"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "hello", "0", "-1", "", "1,700" })
    void testValidateMaxTablesAtUpdate(final String paramValue) throws ExaConnectionAccessException {
        setDerbyConnectionNameProperty();
        final CreateVirtualSchemaRequest request = new CreateVirtualSchemaRequest(createSchemaMetadataInfo());
        when(exaMetadataMock.getConnection("DERBY_CONNECTION")).thenReturn(EXA_CONNECTION_INFORMATION);
        when(exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
        assertDoesNotThrow(() -> this.derbyAdapter.createVirtualSchema(exaMetadataMock, request));

        final Map<String, String> newRawProperties = new HashMap<>();
        newRawProperties.put(TableCountLimit.MAXTABLES_PROPERTY, paramValue);
        final SetPropertiesRequest setPropertiesRequest = new SetPropertiesRequest(createSchemaMetadataInfo(),
                newRawProperties);

        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                () -> this.derbyAdapter.setProperties(exaMetadataMock, setPropertiesRequest));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-43"));
    }

    @Test
    void testCloseWithoutConnectionFactory() {
        final JDBCAdapter jdbcAdapter = createAdapterWithMockDialect();

        jdbcAdapter.connectionFactory = null;
        jdbcAdapter.close();

        assertThat(jdbcAdapter.connectionFactory, nullValue());
    }

    @Test
    void testCloseWithConnectionFactory() {
        final JDBCAdapter jdbcAdapter = createAdapterWithMockDialect();
        final RemoteConnectionFactory connectionFactoryMock = mock(RemoteConnectionFactory.class);
        jdbcAdapter.connectionFactory = connectionFactoryMock;

        jdbcAdapter.close();

        assertAll(
                () -> assertThat(jdbcAdapter.connectionFactory, nullValue()),
                () -> verify(connectionFactoryMock).close());
    }

    @Test
    void pushdownKeepsConnectionCacheAfterSuccessfulRequest() throws AdapterException, SQLException {
        setDerbyConnectionNameProperty();
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "SYSIBM");
        final PushDownRequest request = new PushDownRequest(createSchemaMetadataInfo(),
                TestSqlStatementFactory.createSelectOneFromSysDummy(), null,
                EMPTY_SELECT_LIST_DATA_TYPES);
        when(dialectMock.rewriteQuery(request.getSelect(), EMPTY_SELECT_LIST_DATA_TYPES, exaMetadataMock))
                .thenReturn("IMPORT FROM JDBC");
        final JDBCAdapter jdbcAdapter = createAdapterWithMockDialect();
        final RemoteConnectionFactory connectionFactory = mock(RemoteConnectionFactory.class);
        jdbcAdapter.connectionFactory = connectionFactory;

        final PushDownResponse response = jdbcAdapter.pushdown(exaMetadataMock, request);

        assertAll(() -> assertThat(response.getPushDownSql(), equalTo("IMPORT FROM JDBC")),
                () -> verify(connectionFactory, never()).close());
    }

    private JDBCAdapter createAdapterWithMockDialect() {
        lenient().when(dialectFactoryMock.createSqlDialect(any())).thenReturn(dialectMock);
        lenient().when(dialectMock.getCapabilities()).thenReturn(Capabilities.builder().build());
        return new JDBCAdapter(dialectFactoryMock, new AdapterContext(telemetryClientMock));
    }
}
