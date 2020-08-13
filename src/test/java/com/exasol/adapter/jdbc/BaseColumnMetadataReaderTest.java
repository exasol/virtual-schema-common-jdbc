package com.exasol.adapter.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseIdentifierConverter;
import com.exasol.adapter.dialects.IdentifierCaseHandling;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.metadata.DataType.ExaCharset;
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
        final JdbcTypeDescription jdbcTypeDescription = new JdbcTypeDescription(jdbcType, 0, 0, 0, null);
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription).getExaDataType(), equalTo(ExaDataType.UNSUPPORTED));
    }

    @ValueSource(ints = { Types.NUMERIC })
    @ParameterizedTest
    void testMappingToMaxSizeVarchar(final int jdbcType) {
        final JdbcTypeDescription jdbcTypeDescription = new JdbcTypeDescription(jdbcType, 0, 0, 0, null);
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription),
                equalTo(DataType.createMaximumSizeVarChar(DataType.ExaCharset.UTF8)));
    }

    @ValueSource(ints = { Types.TIME, Types.TIMESTAMP_WITH_TIMEZONE })
    @ParameterizedTest
    void testMappingDateTimeToVarchar(final int jdbcType) {
        final JdbcTypeDescription jdbcTypeDescription = new JdbcTypeDescription(jdbcType, 0, 0, 0, null);
        assertThat(this.reader.mapJdbcType(jdbcTypeDescription), equalTo(DataType.createVarChar(100, ExaCharset.UTF8)));
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
        final JdbcTypeDescription jdbcTypeDescription = new JdbcTypeDescription(8, 10,
                DataType.MAX_EXASOL_DECIMAL_PRECISION + 1, 0, "");
        assertThat(this.reader.mapJdbcTypeNumericToDecimalWithFallbackToDouble(jdbcTypeDescription),
                equalTo(DataType.createDouble()));
    }

    @Test
    void testMapJdbcTypeNumericToDecimalWithFallbackToDoubleReturnsDecimal() {
        final JdbcTypeDescription jdbcTypeDescription = new JdbcTypeDescription(8, 10,
                DataType.MAX_EXASOL_DECIMAL_PRECISION, 0, "");
        assertThat(this.reader.mapJdbcTypeNumericToDecimalWithFallbackToDouble(jdbcTypeDescription),
                equalTo(DataType.createDecimal(DataType.MAX_EXASOL_DECIMAL_PRECISION, 10)));
    }

    @ValueSource(ints = { 256, 65536, 2000000 }) // 2 pow 8, 2 pow 16, max
    @ParameterizedTest
    void mapLongVarchar(final int size) {
        final JdbcTypeDescription typeDescription = new JdbcTypeDescription(Types.LONGVARCHAR, 0, size, 0, "VARCHAR");
        assertThat(this.reader.mapJdbcType(typeDescription), equalTo(DataType.createVarChar(size, ExaCharset.UTF8)));
    }

    @ValueSource(ints = { 2000001, 16777216 }) // max + 1, 2 pow 24
    @ParameterizedTest
    void mapLongVarcharToUnsupportedIfTooLarge(final int size) {
        final JdbcTypeDescription typeDescription = new JdbcTypeDescription(Types.LONGVARCHAR, 0, size, 0, "VARCHAR");
        assertThat(this.reader.mapJdbcType(typeDescription),
                equalTo(DataType.createMaximumSizeVarChar(ExaCharset.UTF8)));
    }
}