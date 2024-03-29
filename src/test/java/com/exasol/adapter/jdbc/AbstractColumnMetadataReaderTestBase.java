package com.exasol.adapter.jdbc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Types;

import com.exasol.adapter.metadata.DataType;

public abstract class AbstractColumnMetadataReaderTestBase {
    protected ColumnMetadataReader columnMetadataReader;

    protected DataType mapJdbcType(final int type) {
        return mapJdbcTypeWithName(type, "");
    }

    protected DataType mapJdbcTypeWithName(final int type, final String typeName) {
        final JDBCTypeDescription jdbcTypeDescription = new JDBCTypeDescription(type, 0, 0, 0, typeName);
        return this.columnMetadataReader.mapJdbcType(jdbcTypeDescription);
    }

    protected void assertNumericMappedToDecimalWithPrecisionAndScale(final int expectedPrecision,
            final int expectedScale) {
        assertThat(mapNumeric(expectedScale, expectedPrecision),
                equalTo(DataType.createDecimal(expectedPrecision, expectedScale)));
    }

    protected DataType mapNumeric(final int expectedScale, final int expectedPrecision) {
        return this.columnMetadataReader
                .mapJdbcType(new JDBCTypeDescription(Types.NUMERIC, expectedScale, expectedPrecision, 0, ""));
    }

    protected void assertNumericMappedToDoubleWithPrecsionAndScale(final int expectedPrecision,
            final int expectedScale) {
        assertThat(mapNumeric(expectedScale, expectedPrecision), equalTo(DataType.createDouble()));
    }
}