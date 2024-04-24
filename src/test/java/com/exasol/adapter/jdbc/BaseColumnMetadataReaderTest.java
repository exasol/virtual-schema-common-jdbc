package com.exasol.adapter.jdbc;

import static com.exasol.adapter.metadata.DataType.ExaCharset.UTF8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseIdentifierConverter;
import com.exasol.adapter.dialects.IdentifierCaseHandling;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.metadata.DataType.ExaDataType;

class BaseColumnMetadataReaderTest {
    private BaseColumnMetadataReader reader;

    @BeforeEach
    void beforeEach() {
        this.reader = new BaseColumnMetadataReader(null, AdapterProperties.emptyProperties(),
                new BaseIdentifierConverter(IdentifierCaseHandling.INTERPRET_AS_UPPER,
                        IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE));
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
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(DataType.createMaximumSizeVarChar(UTF8)));
    }

    @ValueSource(ints = { Types.TIME, Types.TIMESTAMP_WITH_TIMEZONE })
    @ParameterizedTest
    void testMappingDateTimeToVarchar(final int jdbcType) {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(jdbcType, 0, 0, 0, null);
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(DataType.createVarChar(100, UTF8)));
    }

    @Test
    void testGetColumnsFromResultSetSkipsUnsupportedColumns() throws SQLException {
        final ResultSet remoteColumnsMock = mock(ResultSet.class);
        when(remoteColumnsMock.next()).thenReturn(true, true, true, false);
        when(remoteColumnsMock.getString(BaseColumnMetadataReader.NAME_COLUMN)).thenReturn("DATE_COL", "BLOB_COL",
                "DOUBLE_COL");
        when(remoteColumnsMock.getInt(BaseColumnMetadataReader.DATA_TYPE_COLUMN)).thenReturn(Types.DATE, Types.BLOB,
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

    @ValueSource(ints = { 256, 65536, 2000000 }) // 2 pow 8, 2 pow 16, max
    @ParameterizedTest
    void mapLongVarchar(final int size) {
        final JDBCTypeDescription typeDescription = new JDBCTypeDescription(Types.LONGVARCHAR, 0, size, 0, "VARCHAR");
        assertThat(this.reader.mapJdbcType(typeDescription), equalTo(DataType.createVarChar(size, UTF8)));
    }

    @ValueSource(ints = { 2000001, 16777216 }) // max + 1, 2 pow 24
    @ParameterizedTest
    void mapLongVarcharToUnsupportedIfTooLarge(final int size) {
        final JDBCTypeDescription typeDescription = new JDBCTypeDescription(Types.LONGVARCHAR, 0, size, 0, "VARCHAR");
        assertThat(this.reader.mapJdbcType(typeDescription), equalTo(DataType.createMaximumSizeVarChar(UTF8)));
    }

    @Test
    void testGetNumberTypeFromProperty() {
        final BaseColumnMetadataReader reader = new BaseColumnMetadataReader(null,
                new AdapterProperties(Map.of("SOME_PROPERTY", "abc")), new BaseIdentifierConverter(
                        IdentifierCaseHandling.INTERPRET_AS_UPPER, IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE));
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> reader.getNumberTypeFromProperty("SOME_PROPERTY"));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-2"));
    }

    static Stream<Arguments> jdbcDataTypes() {
        return Stream.of( //
                jdbcTypeTest(Types.TINYINT, 0, 2, 0, null, "DECIMAL(2, 0)"),
                jdbcTypeTest(Types.SMALLINT, 0, 4, 0, null, "DECIMAL(4, 0)"),
                jdbcTypeTest(Types.INTEGER, 0, 5, 0, null, "DECIMAL(5, 0)"),
                jdbcTypeTest(Types.INTEGER, 0, 38, 0, null, "VARCHAR(2000000) UTF8"),
                jdbcTypeTest(Types.BIGINT, 0, 6, 0, null, "DECIMAL(6, 0)"),
                jdbcTypeTest(Types.BIGINT, 0, 38, 0, null, "VARCHAR(2000000) UTF8"),
                jdbcTypeTest(Types.DECIMAL, 1, 7, 0, null, "DECIMAL(7, 1)"),
                jdbcTypeTest(Types.DECIMAL, 2, 38, 0, null, "VARCHAR(2000000) UTF8"),
                jdbcTypeTest(Types.REAL, 0, 0, 0, null, "DOUBLE"), jdbcTypeTest(Types.FLOAT, 0, 0, 0, null, "DOUBLE"),
                jdbcTypeTest(Types.DOUBLE, 0, 0, 0, null, "DOUBLE"),

                jdbcTypeTest(Types.VARCHAR, 0, 0, 0, null, "VARCHAR(2000000) UTF8"),
                jdbcTypeTest(Types.VARCHAR, 0, 2000001, 0, null, "VARCHAR(2000000) UTF8"),
                jdbcTypeTest(Types.VARCHAR, 0, 1, 0, null, "VARCHAR(1) UTF8"),
                jdbcTypeTest(Types.NVARCHAR, 0, 2, 0, null, "VARCHAR(2) UTF8"),
                jdbcTypeTest(Types.LONGVARCHAR, 0, 3, 0, null, "VARCHAR(3) UTF8"),
                jdbcTypeTest(Types.LONGNVARCHAR, 0, 4, 0, null, "VARCHAR(4) UTF8"),

                jdbcTypeTest(Types.DATE, 0, 0, 0, null, "DATE"),

                jdbcTypeTest(Types.TIMESTAMP, 0, 3, 0, null, "TIMESTAMP"),
                jdbcTypeTest(Types.TIMESTAMP, 0, 4, 0, null, "TIMESTAMP(4)"),
                jdbcTypeTest(Types.TIMESTAMP_WITH_TIMEZONE, 0, 0, 0, null, "VARCHAR(100) UTF8"),

                jdbcTypeTest(Types.BIT, 0, 0, 0, null, "BOOLEAN"),
                jdbcTypeTest(Types.BOOLEAN, 0, 0, 0, null, "BOOLEAN"),

                jdbcTypeTest(Types.TIME, 0, 0, 0, null, "VARCHAR(100) UTF8"),
                jdbcTypeTest(Types.TIMESTAMP_WITH_TIMEZONE, 0, 0, 0, null, "VARCHAR(100) UTF8"),
                jdbcTypeTest(Types.NUMERIC, 0, 0, 0, null, "VARCHAR(2000000) UTF8"));
    }

    static Arguments jdbcTypeTest(final int jdbcType, final int scale, final int precisionOrSize, final int byteSize,
            final String typeName, final String expectedType) {
        return Arguments.of(new JDBCTypeDescription(jdbcType, scale, precisionOrSize, byteSize, typeName),
                expectedType);
    }

    @ParameterizedTest
    @MethodSource("jdbcDataTypes")
    void mapJdbcType(final JDBCTypeDescription jdbcTypeDescription, final String expectedType) {
        final DataType type = reader.mapJdbcType(jdbcTypeDescription);
        assertThat(type, hasToString(expectedType));
    }
}
