package com.exasol.adapter.dialects;

import static com.exasol.adapter.AdapterProperties.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.dialects.rewriting.SqlGenerationContext;
import com.exasol.adapter.dialects.rewriting.SqlGenerationVisitor;
import com.exasol.adapter.properties.*;
import com.exasol.adapter.sql.ScalarFunction;
import com.exasol.logging.CapturingLogHandler;

class AbstractSqlDialectTest {
    private final CapturingLogHandler capturingLogHandler = new CapturingLogHandler();

    @BeforeEach
    void beforeEach() {
        Logger.getLogger("com.exasol").addHandler(this.capturingLogHandler);
        this.capturingLogHandler.reset();
    }

    @AfterEach
    void afterEach() {
        Logger.getLogger("com.exasol").removeHandler(this.capturingLogHandler);
    }

    @Test
    void testNoConnectionName() {
        final SqlDialect sqlDialect = buildDummySqlDialect(adapterProperties(SCHEMA_NAME_PROPERTY, "MY_SCHEMA"));
        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                sqlDialect::validateProperties);
        assertThat(exception.getMessage(), containsString(
                "E-VSCJDBC-14: Please specify a connection using the property '" + CONNECTION_NAME_PROPERTY + "'."));
    }

    @Test
    void testValidatePropertiesWithWherePortIsString() throws PropertyValidationException {
        final AdapterProperties adapterProperties = minimumPlus(DEBUG_ADDRESS_PROPERTY, "host:port_should_be_a_number");
        assertWarningIssued(adapterProperties, "Illegal debug output port");
    }

    private void assertWarningIssued(final AdapterProperties adapterProperties, final String expectedMessagePart)
            throws PropertyValidationException {
        final SqlDialect sqlDialect = buildDummySqlDialect(adapterProperties);
        sqlDialect.validateProperties();
        assertThat(this.capturingLogHandler.getCapturedData(), containsString(expectedMessagePart));
    }

    @Test
    void testValidatePropertiesWithWherePortTooLow() throws PropertyValidationException {
        final AdapterProperties adapterProperties = minimumPlus(DEBUG_ADDRESS_PROPERTY, "host:0");
        assertWarningIssued(adapterProperties, "Debug output port 0 is out of range");
    }

    @Test
    void testValidatePropertiesWithWherePortTooHigh() throws PropertyValidationException {
        final AdapterProperties adapterProperties = minimumPlus(DEBUG_ADDRESS_PROPERTY, "host:65536");
        assertWarningIssued(adapterProperties, "Debug output port 65536 is out of range");
    }

    @Test
    void testValidDebugAddress() throws PropertyValidationException {
        final SqlDialect sqlDialect = buildDummySqlDialect(minimumPlus(DEBUG_ADDRESS_PROPERTY, "bla:123"));
        sqlDialect.validateProperties();
    }

    @Test
    void testSchemaAndCatalogOptional() throws PropertyValidationException {
        final SqlDialect sqlDialect = buildDummySqlDialect(adapterProperties(CONNECTION_NAME_PROPERTY, "MY_CONN"));
        sqlDialect.validateProperties();
    }

    @ValueSource(strings = { "ab:'ab'", "a'b:'a''b'", "a''b:'a''''b'", "'ab':'''ab'''" })
    @ParameterizedTest
    void testGetLiteralString(final String definition) {
        final int colonPosition = definition.indexOf(':');
        final String original = definition.substring(0, colonPosition);
        final String literal = definition.substring(colonPosition + 1);
        final SqlDialect sqlDialect = buildDummySqlDialect(AdapterProperties.emptyProperties());
        assertThat(sqlDialect.getStringLiteral(original), equalTo(literal));
    }

    @Test
    void testGetStringLiteralWithNull() {
        final SqlDialect sqlDialect = buildDummySqlDialect(AdapterProperties.emptyProperties());
        assertThat(sqlDialect.getStringLiteral(null), equalTo("NULL"));
    }

    @Test
    void testOmitParenthesesFalse() {
        final SqlDialect sqlDialect = buildDummySqlDialect(AdapterProperties.emptyProperties());
        assertThat(sqlDialect.omitParentheses(ScalarFunction.ADD_DAYS), equalTo(false));
    }

    @Test
    void testGetTableCatalogAndSchemaSeparator() {
        final SqlDialect sqlDialect = buildDummySqlDialect(AdapterProperties.emptyProperties());
        assertThat(sqlDialect.getTableCatalogAndSchemaSeparator(), equalTo("."));
    }

    @Test
    void testGetSqlGenerationVisitor() {
        final SqlDialect sqlDialect = buildDummySqlDialect(AdapterProperties.emptyProperties());
        final SqlGenerationContext context = new SqlGenerationContext("catalogName", "schemaName", false);
        assertThat(sqlDialect.getSqlGenerator(context), instanceOf(SqlGenerationVisitor.class));
    }

    @Test
    void testGetScalarFunctionAliases() {
        final SqlDialect sqlDialect = buildDummySqlDialect(AdapterProperties.emptyProperties());
        assertThat(sqlDialect.getScalarFunctionAliases(), anEmptyMap());
    }

    @Test
    void testGetBinaryInfixFunctionAliases() {
        final SqlDialect sqlDialect = buildDummySqlDialect(AdapterProperties.emptyProperties());
        assertAll(() -> assertThat(sqlDialect.getBinaryInfixFunctionAliases().get(ScalarFunction.ADD), equalTo("+")), //
                () -> assertThat(sqlDialect.getBinaryInfixFunctionAliases().get(ScalarFunction.SUB), equalTo("-")), //
                () -> assertThat(sqlDialect.getBinaryInfixFunctionAliases().get(ScalarFunction.MULT), equalTo("*")), //
                () -> assertThat(sqlDialect.getBinaryInfixFunctionAliases().get(ScalarFunction.FLOAT_DIV),
                        equalTo("/")));
    }

    @Test
    void testGetPrefixFunctionAliases() {
        final SqlDialect sqlDialect = buildDummySqlDialect(AdapterProperties.emptyProperties());
        assertThat(sqlDialect.getPrefixFunctionAliases().get(ScalarFunction.NEG), equalTo("-"));
    }

    @Test
    void testUnknownProperty() {
        verifyValidationException("UNSUPPORTED_PROPERTY", "", "E-VSCJDBC-13");
    }

    @ParameterizedTest
    @ValueSource(strings = { SCHEMA_NAME_PROPERTY, CATALOG_NAME_PROPERTY })
    void testStructureElementProperty(final String propertyName) {
        verifyValidationException(propertyName, "", "E-VSCJDBC-44");
    }

    @Test
    void validateDataTypeDetectionStrategy() {
        verifyValidationException(DataTypeDetection.STRATEGY_PROPERTY, "unknown strategy", "E-VSCJDBC-41");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "EXASOL_CALCULATED" })
    void validDataTypeDetectionStrategies(final String strategy) {
        final Map<String, String> raw = new HashMap<>(Map.of(CONNECTION_NAME_PROPERTY, ""));
        if (!strategy.isEmpty()) {
            raw.put(DataTypeDetection.STRATEGY_PROPERTY, strategy);
        }
        final SqlDialect sqlDialect = buildDummySqlDialect(new AdapterProperties(raw));
        assertDoesNotThrow(sqlDialect::validateProperties);
    }

    @Test
    void validDataTypeDetectionStrategiesFromResultSet() {
        final Map<String, String> raw = new HashMap<>(Map.of(CONNECTION_NAME_PROPERTY, ""));
        raw.put(DataTypeDetection.STRATEGY_PROPERTY, "FROM_RESULT_SET");
        final SqlDialect sqlDialect = buildDummySqlDialect(new AdapterProperties(raw));
        assertThrows(PropertyValidationException.class,sqlDialect::validateProperties);
    }

    private void verifyValidationException(final String property, final String value, final String errorcode) {
        final AdapterProperties properties = adapterProperties( //
                CONNECTION_NAME_PROPERTY, "", //
                property, value);
        final SqlDialect sqlDialect = buildDummySqlDialect(properties);
        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                sqlDialect::validateProperties);
        assertAll(() -> assertThat(exception.getMessage(), containsString(errorcode)),
                () -> assertThat(exception.getMessage(), containsString(property)));
    }

    @Test
    void testCheckImportPropertyConsistencyWrongValue() {
        final AdapterProperties properties = adapterProperties( //
                CONNECTION_NAME_PROPERTY, "my_connection", //
                "SOME_PROPERTY", "");
        final PropertyValidator validator = ImportProperty.validator("SOME_PROPERTY", CONNECTION_NAME_PROPERTY);
        final Exception exception = assertThrows(PropertyValidationException.class,
                () -> validator.validate(properties));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-18"));
    }

    @Test
    void testCheckImportPropertyConsistencyNoConnection() {
        final AdapterProperties properties = adapterProperties(//
                CONNECTION_NAME_PROPERTY, "", //
                "SOME_PROPERTY", "TRUE");
        final PropertyValidator validator = ImportProperty.validator("SOME_PROPERTY", CONNECTION_NAME_PROPERTY);
        final Exception exception = assertThrows(PropertyValidationException.class,
                () -> validator.validate(properties));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-17"));
    }

    private static DummySqlDialect buildDummySqlDialect(final AdapterProperties adapterProperties) {
        return new DummySqlDialect(null, adapterProperties, null);
    }

    private AdapterProperties minimumPlus(final String key, final String value) {
        return adapterProperties(CONNECTION_NAME_PROPERTY, "MY_CONN", key, value);
    }

    private AdapterProperties adapterProperties(final String key, final String value) {
        return new AdapterProperties(Map.of(key, value));
    }

    private AdapterProperties adapterProperties(final String k1, final String v1, final String k2, final String v2) {
        return new AdapterProperties(Map.of(k1, v1, k2, v2));
    }
}