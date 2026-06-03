package com.exasol.adapter.jdbc;

import static com.exasol.adapter.metadata.DataType.ExaCharset.UTF8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseIdentifierConverter;
import com.exasol.adapter.dialects.IdentifierCaseHandling;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.metadata.DataType.ExaDataType;
import com.exasol.logging.CapturingLogHandler;

@ExtendWith(MockitoExtension.class)
class BaseColumnMetadataReaderTest {
    private static final DataType MAX_VARCHAR_UTF8 = DataType.createVarChar(DataType.MAX_EXASOL_VARCHAR_SIZE,
            UTF8);

    @Mock
    private ExaMetadata exaMetadataMock;

    private final CapturingLogHandler capturingLogHandler = new CapturingLogHandler();
    private BaseColumnMetadataReader reader;

    @BeforeEach
    void beforeEach() {
        when(this.exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
        Logger.getLogger("com.exasol").addHandler(this.capturingLogHandler);
        this.capturingLogHandler.reset();
        this.reader = new BaseColumnMetadataReader(null, AdapterProperties.emptyProperties(), exaMetadataMock,
                new BaseIdentifierConverter(IdentifierCaseHandling.INTERPRET_AS_UPPER,
                        IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE));
    }

    @AfterEach
    void afterEach() {
        Logger.getLogger("com.exasol").removeHandler(this.capturingLogHandler);
    }

    @ValueSource(ints = { Types.BINARY, Types.CLOB, Types.OTHER, Types.BLOB, Types.NCLOB, Types.LONGVARBINARY,
            Types.VARBINARY, Types.JAVA_OBJECT, Types.DISTINCT, Types.STRUCT, Types.ARRAY, Types.REF, Types.DATALINK,
            Types.SQLXML, Types.NULL, Types.REF_CURSOR })
    @ParameterizedTest
    void testMappingUnsupportedTypesReturnsUnsupportedType(final int jdbcType) {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(jdbcType, 0, 0, 0, null);
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription).getExaDataType(), equalTo(ExaDataType.UNSUPPORTED));
    }

    @Test
    void testMappingNumericToMaxSizeVarchar() {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(Types.NUMERIC, 0, 0, 0, null);
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(MAX_VARCHAR_UTF8));
    }

    @ValueSource(ints = { Types.TIME, Types.TIMESTAMP_WITH_TIMEZONE })
    @ParameterizedTest
    void testMappingDateTimeToVarchar(final int jdbcType) {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(jdbcType, 0, 0, 0, null);
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(DataType.createVarChar(100, UTF8)));
    }

    @ValueSource(ints = { Types.CHAR, Types.NCHAR })
    @ParameterizedTest
    void testParseCharZeroMapsToMaximumSizeVarchar(final int typeId) {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(typeId, 0, 0, 0, "");
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(MAX_VARCHAR_UTF8));
    }

    @ValueSource(ints = { Types.CHAR, Types.NCHAR })
    @ParameterizedTest
    void testParseCharNegativeMapsToMaximumSizeVarchar(final int typeId) {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(typeId, 1, -1, 1, "");
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(MAX_VARCHAR_UTF8));
    }

    @Test
    void testGetColumnsFromResultSetSkipsUnsupportedColumns() throws SQLException {
        final ResultSet remoteColumnsMock = mock(ResultSet.class);
        when(remoteColumnsMock.next()).thenReturn(true, true, true, false);
        lenient().when(remoteColumnsMock.getString(BaseColumnMetadataReader.NAME_COLUMN)).thenReturn("DATE_COL", "BLOB_COL",
                "DOUBLE_COL");
        lenient().when(remoteColumnsMock.getInt(BaseColumnMetadataReader.DATA_TYPE_COLUMN)).thenReturn(Types.DATE, Types.BLOB,
                Types.DOUBLE);
        final List<ColumnMetadata> columns = this.reader.getColumnsFromResultSet(remoteColumnsMock);
        final List<ExaDataType> columnTypes = columns //
                .stream() //
                .map(column -> column.getType().getExaDataType()) //
                .collect(Collectors.toList());
        assertThat(columnTypes, containsInAnyOrder(ExaDataType.DATE, ExaDataType.DOUBLE));
    }

    @Test
    void testMapJdbcTypeNumericToDecimalWithFallbackToDoubleReturnsDouble() {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(8, 10,
                DataType.MAX_EXASOL_DECIMAL_PRECISION + 1, 0, "");
        assertThat(this.reader.mapJdbcTypeNumericToDecimalWithFallbackToDouble(jdbcTypeDescription),
                equalTo(DataType.createDouble()));
    }

    @Test
    void testMapJdbcTypeNumericToDecimalWithFallbackToDoubleReturnsDecimal() {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(8, 10,
                DataType.MAX_EXASOL_DECIMAL_PRECISION, 0, "");
        assertThat(this.reader.mapJdbcTypeNumericToDecimalWithFallbackToDouble(jdbcTypeDescription),
                equalTo(DataType.createDecimal(DataType.MAX_EXASOL_DECIMAL_PRECISION, 10)));
    }

    @ValueSource(ints = { Types.CHAR, Types.NCHAR })
    @ParameterizedTest
    void testParseCharExceedsMaxCharSizeLogsWarningAndMapsToVarchar(final int typeId) {
        final int size = DataType.MAX_EXASOL_CHAR_SIZE + 1;
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(typeId, 0, size, 0, "");
        assertAll(() -> assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(DataType.createVarChar(size, UTF8))),
                () -> assertThat(this.capturingLogHandler.getCapturedData(), containsString(
                        "W-VSCJDBC-54: CHAR size 2001 exceeds the maximum Exasol CHAR size 2000. Mapping to VARCHAR(2001) instead")));
    }

    @ValueSource(ints = { Types.CHAR, Types.NCHAR })
    @ParameterizedTest
    void testParseCharExceedsMaxVarCharSizeLogsWarningAndMapsToMaximumSizeVarchar(final int typeId) {
        final int size = DataType.MAX_EXASOL_VARCHAR_SIZE + 1;
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(typeId, 0, size, 0, "");
        assertAll(() -> assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(MAX_VARCHAR_UTF8)),
                () -> assertThat(this.capturingLogHandler.getCapturedData(), containsString(
                        "W-VSCJDBC-55: CHAR size 2000001 exceeds the maximum Exasol VARCHAR size 2000000. Mapping to maximum-size VARCHAR instead")));
    }

    @ValueSource(ints = { 256, 65536, 2000000 }) // 2 pow 8, 2 pow 16, max
    @ParameterizedTest
    void mapLongVarchar(final int size) {
        final JDBCTypeDescription typeDescription = new JDBCTypeDescription(Types.LONGVARCHAR, 0, size, 0, "VARCHAR");
        assertThat(this.reader.mapJdbcType(typeDescription), equalTo(DataType.createVarChar(size, UTF8)));
    }

    @ValueSource(ints = { Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR })
    @ParameterizedTest
    void testParseVarCharZeroMapsToMaximumSizeVarchar(final int typeId) {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(typeId, 0, 0, 0, "");
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(MAX_VARCHAR_UTF8));
    }

    @ValueSource(ints = { Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR })
    @ParameterizedTest
    void testParseVarCharExceedsMaxSizeLogsWarningAndMapsToMaximumSizeVarchar(final int typeId) {
        final int size = DataType.MAX_EXASOL_VARCHAR_SIZE + 1;
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(typeId, 0, size, 0, "");
        assertAll(() -> assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(MAX_VARCHAR_UTF8)),
                () -> assertThat(this.capturingLogHandler.getCapturedData(), containsString(
                        "W-VSCJDBC-53: VARCHAR size 2000001 exceeds the maximum Exasol VARCHAR size 2000000. Mapping to maximum-size VARCHAR instead")));
    }

    @ValueSource(ints = { 2000001, 16777216 }) // max + 1, 2 pow 24
    @ParameterizedTest
    void mapLongVarcharToMaximumSizeVarcharIfTooLarge(final int size) {
        final JDBCTypeDescription typeDescription = new JDBCTypeDescription(Types.LONGVARCHAR, 0, size, 0, "VARCHAR");
        assertThat(this.reader.mapJdbcType(typeDescription), equalTo(MAX_VARCHAR_UTF8));
    }

    @Test
    void testGetNumberTypeFromProperty() {
        final BaseColumnMetadataReader metadataReader = new BaseColumnMetadataReader(null,
                new AdapterProperties(Map.of("SOME_PROPERTY", "abc")), exaMetadataMock, new BaseIdentifierConverter(
                        IdentifierCaseHandling.INTERPRET_AS_UPPER, IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE));
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> metadataReader.getNumberTypeFromProperty("SOME_PROPERTY"));
        assertThat(exception.getMessage(), Matchers.startsWith(
                "E-VSCJDBC-2: Unable to parse adapter property SOME_PROPERTY value 'abc' into a number precision and scale."));
    }
}
