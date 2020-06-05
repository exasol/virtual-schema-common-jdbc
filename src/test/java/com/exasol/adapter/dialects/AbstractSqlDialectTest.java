package com.exasol.adapter.dialects;

import static com.exasol.adapter.AdapterProperties.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.sql.AggregateFunction;
import com.exasol.adapter.sql.ScalarFunction;
import com.exasol.logging.CapturingLogHandler;

class AbstractSqlDialectTest {
    private Map<String, String> rawProperties;
    private final CapturingLogHandler capturingLogHandler = new CapturingLogHandler();

    @BeforeEach
    void beforeEach() {
        Logger.getLogger("com.exasol").addHandler(this.capturingLogHandler);
        this.capturingLogHandler.reset();
        this.rawProperties = new HashMap<>();
    }

    @AfterEach
    void afterEach() {
        Logger.getLogger("com.exasol").removeHandler(this.capturingLogHandler);
    }

    @Test
    void testNoConnectionName() {
        this.rawProperties.put(SQL_DIALECT_PROPERTY, "GENERIC");
        this.rawProperties.put(SCHEMA_NAME_PROPERTY, "MY_SCHEMA");
        final AdapterProperties adapterProperties = new AdapterProperties(this.rawProperties);
        final SqlDialect sqlDialect = new DummySqlDialect(null, adapterProperties);
        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                sqlDialect::validateProperties);
        assertThat(exception.getMessage(),
                containsString("Please specify a connection using the property \"" + CONNECTION_NAME_PROPERTY + "\"."));
    }

    private void getMinimumMandatory() {
        this.rawProperties.put(SQL_DIALECT_PROPERTY, "GENERIC");
        this.rawProperties.put(CONNECTION_NAME_PROPERTY, "MY_CONN");
    }

    @Test
    void testValidatePropertiesWithWherePortIsString() throws PropertyValidationException {
        this.rawProperties.put(DEBUG_ADDRESS_PROPERTY, "host:port_should_be_a_number");
        assertWarningIssued("Illegal debug output port");
    }

    private void assertWarningIssued(final String expectedMessagePart) throws PropertyValidationException {
        getMinimumMandatory();
        final AdapterProperties adapterProperties = new AdapterProperties(this.rawProperties);
        final SqlDialect sqlDialect = new DummySqlDialect(null, adapterProperties);
        sqlDialect.validateProperties();
        assertThat(this.capturingLogHandler.getCapturedData(), containsString(expectedMessagePart));
    }

    @Test
    void testValidatePropertiesWithWherePortTooLow() throws PropertyValidationException {
        this.rawProperties.put(DEBUG_ADDRESS_PROPERTY, "host:0");
        assertWarningIssued("Debug output port 0 is out of range");
    }

    @Test
    void testValidatePropertiesWithWherePortTooHigh() throws PropertyValidationException {
        this.rawProperties.put(DEBUG_ADDRESS_PROPERTY, "host:65536");
        assertWarningIssued("Debug output port 65536 is out of range");
    }

    @Test
    void testValidDebugAddress() throws PropertyValidationException {
        getMinimumMandatory();
        this.rawProperties.put(DEBUG_ADDRESS_PROPERTY, "bla:123");
        final AdapterProperties adapterProperties = new AdapterProperties(this.rawProperties);
        final SqlDialect sqlDialect = new DummySqlDialect(null, adapterProperties);
        sqlDialect.validateProperties();
    }

    @Test
    void testSchemaAndCatalogOptional() throws PropertyValidationException {
        this.rawProperties.put(SQL_DIALECT_PROPERTY, "GENERIC");
        this.rawProperties.put(CONNECTION_NAME_PROPERTY, "MY_CONN");
        final AdapterProperties adapterProperties = new AdapterProperties(this.rawProperties);
        final SqlDialect sqlDialect = new DummySqlDialect(null, adapterProperties);
        sqlDialect.validateProperties();
    }

    @Test
    void testInvalidExceptionHandling() {
        getMinimumMandatory();
        this.rawProperties.put(EXCEPTION_HANDLING_PROPERTY, "IGNORE_ALL");
        final AdapterProperties adapterProperties = new AdapterProperties(this.rawProperties);
        final SqlDialect sqlDialect = new DummySqlDialect(null, adapterProperties);
        final PropertyValidationException exception = assertThrows(PropertyValidationException.class,
                sqlDialect::validateProperties);
        assertThat(exception.getMessage(), containsString(
                "Invalid value 'IGNORE_ALL' for property EXCEPTION_HANDLING. Choose one of: IGNORE_INVALID_VIEWS, NONE"));
    }

    @ValueSource(strings = { "ab:\'ab\'", "a'b:'a''b'", "a''b:'a''''b'", "'ab':'''ab'''" })
    @ParameterizedTest
    void testGetLiteralString(final String definition) {
        final int colonPosition = definition.indexOf(':');
        final String original = definition.substring(0, colonPosition);
        final String literal = definition.substring(colonPosition + 1);
        final AdapterProperties adapterProperties = new AdapterProperties(this.rawProperties);
        final SqlDialect sqlDialect = new DummySqlDialect(null, adapterProperties);
        assertThat(sqlDialect.getStringLiteral(original), equalTo(literal));
    }

    @Test
    void testGetStringLiteralWithNull() {
        final SqlDialect sqlDialect = new DummySqlDialect(null, AdapterProperties.emptyProperties());
        assertThat(sqlDialect.getStringLiteral(null), equalTo("NULL"));
    }

    @Test
    void testOmitParenthesesFalse() {
        final SqlDialect sqlDialect = new DummySqlDialect(null, AdapterProperties.emptyProperties());
        assertThat(sqlDialect.omitParentheses(ScalarFunction.ADD_DAYS), equalTo(false));
    }

    @Test
    void testGetTableCatalogAndSchemaSeparator() {
        final SqlDialect sqlDialect = new DummySqlDialect(null, AdapterProperties.emptyProperties());
        assertThat(sqlDialect.getTableCatalogAndSchemaSeparator(), equalTo("."));
    }

    @Test
    void testGetSqlGenerationVisitor() {
        final SqlDialect sqlDialect = new DummySqlDialect(null, AdapterProperties.emptyProperties());
        final SqlGenerationContext context = new SqlGenerationContext("catalogName", "schemaName", false);
        assertThat(sqlDialect.getSqlGenerationVisitor(context), instanceOf(SqlGenerationVisitor.class));
    }

    @Test
    void testGetScalarFunctionAliases() {
        final SqlDialect sqlDialect = new DummySqlDialect(null, AdapterProperties.emptyProperties());
        assertThat(sqlDialect.getScalarFunctionAliases(), anEmptyMap());
    }

    @Test
    void testGetBinaryInfixFunctionAliases() {
        final SqlDialect sqlDialect = new DummySqlDialect(null, AdapterProperties.emptyProperties());
        assertAll(() -> assertThat(sqlDialect.getBinaryInfixFunctionAliases().get(ScalarFunction.ADD), equalTo("+")), //
                () -> assertThat(sqlDialect.getBinaryInfixFunctionAliases().get(ScalarFunction.SUB), equalTo("-")), //
                () -> assertThat(sqlDialect.getBinaryInfixFunctionAliases().get(ScalarFunction.MULT), equalTo("*")), //
                () -> assertThat(sqlDialect.getBinaryInfixFunctionAliases().get(ScalarFunction.FLOAT_DIV),
                        equalTo("/")));
    }

    @Test
    void testGetPrefixFunctionAliases() {
        final SqlDialect sqlDialect = new DummySqlDialect(null, AdapterProperties.emptyProperties());
        assertThat(sqlDialect.getPrefixFunctionAliases().get(ScalarFunction.NEG), equalTo("-"));
    }
}