package com.exasol.adapter.adapternotes;

import java.util.Objects;

/**
 * Holds the column adapter notes.
 */
public final class ColumnAdapterNotes {
    private final int jdbcDataType;

    /**
     * Create a new instance of the {@link ColumnAdapterNotes}.
     *
     * @param jdbcDataType JDBC data type number
     */
    public ColumnAdapterNotes(final int jdbcDataType) {
        this.jdbcDataType = jdbcDataType;
    }

    /**
     * Get JDBC data type.
     *
     * @return JDBC data type as an number
     */
    public int getJdbcDataType() {
        return this.jdbcDataType;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ColumnAdapterNotes)) {
            return false;
        }
        final ColumnAdapterNotes that = (ColumnAdapterNotes) other;
        return this.jdbcDataType == that.jdbcDataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.jdbcDataType);
    }
}