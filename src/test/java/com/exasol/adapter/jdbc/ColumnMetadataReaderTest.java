package com.exasol.adapter.jdbc;

import static com.exasol.adapter.jdbc.BaseColumnMetadataReader.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.*;
import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseIdentifierConverter;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.metadata.DataType.ExaCharset;

@ExtendWith(MockitoExtension.class)
class ColumnMetadataReaderTest {
    private static final SQLException FAKE_SQL_EXCEPTION = new SQLException("Fake exception");
    private static final DataType TYPE_MAX_VARCHAR_UTF8 = DataType.createVarChar(DataType.MAX_EXASOL_VARCHAR_SIZE,
            ExaCharset.UTF8);
    private static final DataType TYPE_MAX_VARCHAR_ASCII = DataType.createVarChar(DataType.MAX_EXASOL_VARCHAR_SIZE,
            ExaCharset.ASCII);
    @Mock
    private Connection connectionMock;
    @Mock
    private DatabaseMetaData remoteMetadataMock;
    @Mock
    private ResultSet columnsMock;

    @BeforeEach
    void beforeEach() throws SQLException {
        when(this.connectionMock.getMetaData()).thenReturn(this.remoteMetadataMock);
    }

    @Test
    void testMapColumnsSingleColumn() throws SQLException {
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.BOOLEAN);
        final ColumnMetadata column = mapSingleMockedColumn("BOOLEAN");
        assertAll(() -> assertThat(column.getName(), equalTo("BOOLEAN_COLUMN")),
                () -> assertThat(column.getType(), equalTo(DataType.createBool())),
                () -> assertThat(column.isNullable(), equalTo(true)),
                () -> assertThat(column.isIdentity(), equalTo(false)));
    }

    private ColumnMetadata mapSingleMockedColumn() throws SQLException {
        return mapSingleMockedColumn("ORIGINAL DATA TYPE");
    }

    private ColumnMetadata mapSingleMockedColumn(final String originalTypeName) throws SQLException {
        when(this.columnsMock.next()).thenReturn(true, false);
        when(this.columnsMock.getString(NAME_COLUMN)).thenReturn(originalTypeName + "_COLUMN");
        when(this.columnsMock.getString(TYPE_NAME_COLUMN)).thenReturn(originalTypeName);
        when(this.remoteMetadataMock.getColumns(null, null, "THE\\_TABLE", "%")).thenReturn(this.columnsMock);
        final List<ColumnMetadata> columns = mapMockedColumns(this.columnsMock);
        return columns.get(0);
    }

    private List<ColumnMetadata> mapMockedColumns(final ResultSet columnsMock)
            throws RemoteMetadataReaderException, SQLException {
        when(this.remoteMetadataMock.getColumns(null, null, "THE\\_TABLE", "%")).thenReturn(columnsMock);
        return createDefaultColumnMetadataReader().mapColumns("THE_TABLE");
    }

    protected BaseColumnMetadataReader createDefaultColumnMetadataReader() {
        return new BaseColumnMetadataReader(this.connectionMock, AdapterProperties.emptyProperties(),
                BaseIdentifierConverter.createDefault());
    }

    @Test
    void testParseBoolean() throws SQLException, RemoteMetadataReaderException {
        assertSqlTypeConvertedToExasolType(Types.BOOLEAN, DataType.createBool());
    }

    private void assertSqlTypeConvertedToExasolType(final int typeId, final DataType expectedDataType)
            throws SQLException {
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(typeId);
        assertThat(mapSingleMockedColumn().getType(), equalTo(expectedDataType));
    }

    @Test
    void testParseDate() throws SQLException, RemoteMetadataReaderException {
        assertSqlTypeConvertedToExasolType(Types.DATE, DataType.createDate());
    }

    @ValueSource(ints = { Types.REAL, Types.FLOAT, Types.DOUBLE })
    @ParameterizedTest
    void testParseDouble(final int typeId) throws SQLException, RemoteMetadataReaderException {
        assertSqlTypeConvertedToExasolType(typeId, DataType.createDouble());
    }

    @ValueSource(ints = { Types.CHAR, Types.NCHAR })
    @ParameterizedTest
    void testParseCharWithSize(final int typeId) throws SQLException, RemoteMetadataReaderException {
        final int size = 70;
        assertSqlTypeWithPrecisionConvertedToExasolType(typeId, size, 0, DataType.createChar(size, ExaCharset.UTF8));
    }

    private void assertSqlTypeWithPrecisionConvertedToExasolType(final int typeId, final int size, final int scale,
            final DataType expectedDataType) throws SQLException {
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(typeId);
        when(this.columnsMock.getInt(SIZE_COLUMN)).thenReturn(size);
        when(this.columnsMock.getInt(SCALE_COLUMN)).thenReturn(scale);
        assertThat("Mapping java.sql.Type number " + typeId, mapSingleMockedColumn().getType(),
                equalTo(expectedDataType));
    }

    @ValueSource(ints = { Types.CHAR, Types.NCHAR })
    @ParameterizedTest
    void testParseCharAsciiWithSize(final int typeId) throws SQLException, RemoteMetadataReaderException {
        final int size = 70;
        when(this.columnsMock.getInt(CHAR_OCTET_LENGTH_COLUMN)).thenReturn(size);
        assertSqlTypeWithPrecisionConvertedToExasolType(typeId, size, 0, DataType.createChar(size, ExaCharset.ASCII));
    }

    @ValueSource(ints = { Types.CHAR, Types.NCHAR })
    @ParameterizedTest
    void testParseCharExceedsMaxCharSize(final int typeId) throws SQLException, RemoteMetadataReaderException {
        final int size = DataType.MAX_EXASOL_CHAR_SIZE + 1;
        assertSqlTypeWithPrecisionConvertedToExasolType(typeId, size, 0, DataType.createVarChar(size, ExaCharset.UTF8));
    }

    @ValueSource(ints = { Types.CHAR, Types.NCHAR })
    @ParameterizedTest
    void testParseCharExceedsMaxVarCharSize(final int typeId) throws SQLException, RemoteMetadataReaderException {
        assertSqlTypeWithPrecisionConvertedToExasolType(typeId, DataType.MAX_EXASOL_VARCHAR_SIZE + 1, 0,
                TYPE_MAX_VARCHAR_UTF8);
    }

    @ValueSource(ints = { Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR })
    @ParameterizedTest
    void testParseVarChar(final int typeId) throws SQLException {
        assertSqlTypeConvertedToExasolType(typeId, TYPE_MAX_VARCHAR_ASCII);
    }

    @ValueSource(ints = { Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR })
    @ParameterizedTest
    void testParseVarCharWithSize(final int typeId) throws SQLException {
        final int size = 40;
        assertSqlTypeWithPrecisionConvertedToExasolType(typeId, size, 0, DataType.createVarChar(size, ExaCharset.UTF8));
    }

    @ValueSource(ints = { Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR })
    @ParameterizedTest
    void testParseVarCharAsciiWithSize(final int typeId) throws SQLException {
        final int size = 80;
        when(this.columnsMock.getInt(CHAR_OCTET_LENGTH_COLUMN)).thenReturn(size);
        assertSqlTypeWithPrecisionConvertedToExasolType(typeId, size, 0,
                DataType.createVarChar(size, ExaCharset.ASCII));
    }

    @ValueSource(ints = { Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR })
    @ParameterizedTest
    void testParseVarCharExceedsMaxSize(final int typeId) throws SQLException {
        assertSqlTypeWithPrecisionConvertedToExasolType(typeId, DataType.MAX_EXASOL_VARCHAR_SIZE + 1, 0,
                TYPE_MAX_VARCHAR_UTF8);
    }

    @Test
    void testParseTimestamp() throws SQLException {
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.TIMESTAMP);
        assertThat(mapSingleMockedColumn("TIMESTAMP").getType().toString(), equalTo("TIMESTAMP"));
    }

    @ValueSource(ints = { Types.TINYINT, Types.SMALLINT })
    @ParameterizedTest
    void testSmallInteger(final int typeId) throws SQLException {
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(typeId);
        final DataType type = mapSingleMockedColumn().getType();
        assertThat(type, equalTo(DataType.createDecimal(9, 0)));
    }

    @ValueSource(ints = { Types.TINYINT, Types.SMALLINT })
    @ParameterizedTest
    void testSmallIntegerWithPrecision(final int typeId) throws SQLException {
        final int precision = 3;
        assertSqlTypeWithPrecisionConvertedToExasolType(typeId, precision, 0, DataType.createDecimal(precision, 0));
    }

    @Test
    void testInteger() throws SQLException {
        assertSqlTypeConvertedToExasolType(Types.INTEGER, DataType.createDecimal(18, 0));
    }

    @Test
    void testIntegerWithPrecision() throws SQLException {
        final int precision = 17;
        assertSqlTypeWithPrecisionConvertedToExasolType(Types.INTEGER, precision, 0,
                DataType.createDecimal(precision, 0));
    }

    @ValueSource(ints = { Types.INTEGER, Types.BIGINT })
    @ParameterizedTest
    void testIntegerExceedsMaxPrecision(final int typeId) throws SQLException {
        assertSqlTypeWithPrecisionConvertedToExasolType(typeId, DataType.MAX_EXASOL_VARCHAR_SIZE + 1, 0,
                TYPE_MAX_VARCHAR_UTF8);
    }

    @Test
    void testBigInteger() throws SQLException {
        assertSqlTypeConvertedToExasolType(Types.BIGINT,
                DataType.createDecimal(DataType.MAX_EXASOL_DECIMAL_PRECISION, 0));
    }

    @Test
    void testBigIntegerWithPrecision() throws SQLException {
        final int precision = 35;
        assertSqlTypeWithPrecisionConvertedToExasolType(Types.BIGINT, precision, 0,
                DataType.createDecimal(precision, 0));
    }

    @Test
    void testDecimal() throws SQLException {
        final int precision = 33;
        final int scale = 9;
        assertSqlTypeWithPrecisionConvertedToExasolType(Types.DECIMAL, precision, scale,
                DataType.createDecimal(precision, scale));
    }

    @Test
    void testDecimalExceedsMaxPrecision() throws SQLException {
        final int scale = 17;
        assertSqlTypeWithPrecisionConvertedToExasolType(Types.DECIMAL, DataType.MAX_EXASOL_VARCHAR_SIZE + 1, scale,
                TYPE_MAX_VARCHAR_UTF8);
    }

    @Test
    void testNumeric() throws SQLException {
        assertSqlTypeConvertedToExasolType(Types.NUMERIC, TYPE_MAX_VARCHAR_UTF8);
    }

    @Test
    void testTime() throws SQLException {
        assertSqlTypeConvertedToExasolType(Types.TIME, DataType.createVarChar(100, ExaCharset.UTF8));
    }

    @CsvSource({ RemoteMetadataReaderConstants.JDBC_FALSE + ", false",
            RemoteMetadataReaderConstants.JDBC_TRUE + ", true", "'', true" })
    @ParameterizedTest
    void testMapNotNullableColumn(final String jdbcNullable, final String nullable) throws SQLException {
        mockColumnNotNullable(jdbcNullable);
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.DOUBLE);
        assertThat("JDBC string \"" + jdbcNullable + "\" interpreted as nullable", mapSingleMockedColumn().isNullable(),
                equalTo(Boolean.parseBoolean(nullable)));
    }

    private void mockColumnNotNullable(final String jdbcNullable) throws SQLException {
        when(this.columnsMock.getString(NULLABLE_COLUMN)).thenReturn(jdbcNullable);
    }

    @Test
    void testMapColumnCountsAsNullableWhenNullabilityCheckThrowsSqlException() throws SQLException {
        mockCheckingNullabilityThrowsSqlException();
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.DOUBLE);
        assertThat(mapSingleMockedColumn("DOUBLE").isNullable(), equalTo(true));
    }

    private void mockCheckingNullabilityThrowsSqlException() throws SQLException {
        when(this.columnsMock.getString(NULLABLE_COLUMN)).thenThrow(FAKE_SQL_EXCEPTION);
    }

    @CsvSource({ RemoteMetadataReaderConstants.JDBC_FALSE + ", false",
            RemoteMetadataReaderConstants.JDBC_TRUE + ", true", "'', false" })
    @ParameterizedTest
    void testMapIdentityColumn(final String jdbcAutoIncrement, final String identity) throws SQLException {
        mockColumnAutoIncrement(jdbcAutoIncrement);
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.DOUBLE);
        assertThat("JDBC string \"" + jdbcAutoIncrement + "\" interpreted as auto-increment on",
                mapSingleMockedColumn().isIdentity(), equalTo(Boolean.parseBoolean(identity)));
    }

    private void mockColumnAutoIncrement(final String jdbcAutoIncrement) throws SQLException {
        when(this.columnsMock.getString(AUTOINCREMENT_COLUMN)).thenReturn(jdbcAutoIncrement);
        when(this.columnsMock.getString(NULLABLE_COLUMN)).thenReturn("true");
    }

    @Test
    void testMapColumnConsideredNotIdentityWhenAutoIncrementCheckThrowsSqlException() throws SQLException {
        mockCheckingAutoIncrementThrowsSqlException();
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.DOUBLE);
        assertThat(mapSingleMockedColumn("DOUBLE").isIdentity(), equalTo(false));
    }

    private void mockCheckingAutoIncrementThrowsSqlException() throws SQLException {
        when(this.columnsMock.getString(AUTOINCREMENT_COLUMN)).thenThrow(FAKE_SQL_EXCEPTION);
        when(this.columnsMock.getString(NULLABLE_COLUMN)).thenReturn("true");
    }

    @Test
    void testMapColumnWithDefault() throws SQLException {
        final String defaultValue = "this is a default value";
        mockDefaultValue(defaultValue);
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.VARCHAR);
        assertThat(mapSingleMockedColumn("VARCHAR").getDefaultValue(), equalTo(defaultValue));
    }

    private void mockDefaultValue(final String defaultValue) throws SQLException {
        when(this.columnsMock.getString(DEFAULT_VALUE_COLUMN)).thenReturn(defaultValue);
        when(this.columnsMock.getString(NULLABLE_COLUMN)).thenReturn("true");
        when(this.columnsMock.getString(AUTOINCREMENT_COLUMN)).thenReturn("true");
    }

    @Test
    void testMapColumnWithDefaultNullToEmptyString() throws SQLException {
        mockDefaultValue(null);
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.VARCHAR);
        assertThat(mapSingleMockedColumn("VARCHAR").getDefaultValue(), equalTo(""));
    }

    @Test
    void testMapColumnDefaultValueWhenReadingDefaultThrowsSqlException() throws SQLException {
        mockReadingDefaultThrowsSqlException();
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.VARCHAR);
        assertThat(mapSingleMockedColumn("VARCHAR").getDefaultValue(), equalTo(""));
    }

    private void mockReadingDefaultThrowsSqlException() throws SQLException {
        when(this.columnsMock.getString(DEFAULT_VALUE_COLUMN)).thenThrow(FAKE_SQL_EXCEPTION);
        when(this.columnsMock.getString(NULLABLE_COLUMN)).thenReturn("true");
        when(this.columnsMock.getString(AUTOINCREMENT_COLUMN)).thenReturn("true");
    }

    @CsvSource({ "Comment, Comment", "'', ''" })
    @ParameterizedTest
    void testMapColumnWithComment(final String input, final String expected) throws SQLException {
        mockComment(input);
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.VARCHAR);
        assertThat(mapSingleMockedColumn("VARCHAR").getComment(), equalTo(expected));
    }

    @Test
    void testMapColumnWithTypeNameNull() throws SQLException {
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.DOUBLE);
        when(this.columnsMock.next()).thenReturn(true, false);
        when(this.columnsMock.getString(NAME_COLUMN)).thenReturn("DOUBLE_COLUMN");
        when(this.columnsMock.getString(TYPE_NAME_COLUMN)).thenReturn(null);
        when(this.remoteMetadataMock.getColumns(null, null, "THE\\_TABLE", "%")).thenReturn(this.columnsMock);
        final List<ColumnMetadata> columns = mapMockedColumns(this.columnsMock);
        final ColumnMetadata columnMetadata = columns.get(0);
        assertThat(columnMetadata.getOriginalTypeName(), equalTo(""));
    }

    private void mockComment(final String comment) throws SQLException {
        when(this.columnsMock.getString(REMARKS_COLUMN)).thenReturn(comment);
        when(this.columnsMock.getString(NULLABLE_COLUMN)).thenReturn("true");
        when(this.columnsMock.getString(AUTOINCREMENT_COLUMN)).thenReturn("true");
        when(this.columnsMock.getString(DEFAULT_VALUE_COLUMN)).thenReturn("value");
    }

    @Test
    void testMapColumnWithCommentNullToEmptyString() throws SQLException {
        mockComment(null);
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.VARCHAR);
        when(this.columnsMock.getString(AUTOINCREMENT_COLUMN)).thenReturn("true");
        when(this.columnsMock.getString(DEFAULT_VALUE_COLUMN)).thenReturn("value");
        when(this.columnsMock.getString(NULLABLE_COLUMN)).thenReturn("true");
        assertThat(mapSingleMockedColumn("VARCHAR").getComment(), equalTo(""));
    }

    @Test
    void testMapColumnCommentWhenReadingDefaultThrowsSqlException() throws SQLException {
        mockReadingCommentThrowsSqlException();
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.DOUBLE);
        when(this.columnsMock.getString(NULLABLE_COLUMN)).thenReturn("true");
        when(this.columnsMock.getString(AUTOINCREMENT_COLUMN)).thenReturn("true");
        when(this.columnsMock.getString(DEFAULT_VALUE_COLUMN)).thenReturn("value");
        assertThat(mapSingleMockedColumn("DOUBLE").getComment(), equalTo(""));
    }

    private void mockReadingCommentThrowsSqlException() throws SQLException {
        when(this.columnsMock.getString(REMARKS_COLUMN)).thenThrow(FAKE_SQL_EXCEPTION);
    }

    @Test
    void testMapColumnsWrapsSqlException() throws SQLException {
        when(this.connectionMock.getMetaData()).thenThrow(FAKE_SQL_EXCEPTION);
        final BaseColumnMetadataReader defaultColumnMetadataReader = createDefaultColumnMetadataReader();
        final RemoteMetadataReaderException exception = assertThrows(RemoteMetadataReaderException.class,
                () -> defaultColumnMetadataReader.mapColumns(""));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-1"));

    }

    @Test
    void testMapColumnAdapterNotes() throws SQLException, JSONException {
        when(this.columnsMock.getInt(DATA_TYPE_COLUMN)).thenReturn(Types.DOUBLE);
        JSONAssert.assertEquals("{\"jdbcDataType\":8,\"typeName\":\"DOUBLE\"}",
                mapSingleMockedColumn("DOUBLE").getAdapterNotes(), true);
    }
}