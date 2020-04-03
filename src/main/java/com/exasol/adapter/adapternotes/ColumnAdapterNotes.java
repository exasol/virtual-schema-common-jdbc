package com.exasol.adapter.adapternotes;

import java.util.Objects;

import javax.annotation.processing.Generated;

/**
 * Holds the column adapter notes.
 */
public final class ColumnAdapterNotes {
    private final int jdbcDataType;
    private final String typeName;

    private ColumnAdapterNotes(final Builder builder) {
        this.jdbcDataType = builder.jdbcDataType;
        this.typeName = builder.typeName;
    }

    /**
     * Get JDBC data type.
     *
     * @return JDBC data type as a number
     */
    public int getJdbcDataType() {
        return this.jdbcDataType;
    }

    /**
     * Get the type name.
     *
     * @return name of the type
     */
    public String getTypeName() {
        return this.typeName;
    }

    @Generated("org.eclipse.Eclipse")
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ColumnAdapterNotes)) {
            return false;
        }
        final ColumnAdapterNotes other = (ColumnAdapterNotes) object;
        if (this.jdbcDataType != other.jdbcDataType) {
            return false;
        }
        if (this.typeName == null) {
            if (other.typeName != null) {
                return false;
            }
        } else if (!this.typeName.equals(other.typeName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.jdbcDataType, this.typeName);
    }

    /**
     * Create a new builder for {@link ColumnAdapterNotes}.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ColumnAdapterNotes}.
     */
    public static final class Builder {
        private int jdbcDataType;
        private String typeName;

        /**
         * Set the JDBC data type.
         *
         * @param jdbcDataType JDBC data type
         * @return builder instance for fluent programming
         */
        public Builder jdbcDataType(final int jdbcDataType) {
            this.jdbcDataType = jdbcDataType;
            return this;
        }

        /**
         * Set the type name.
         *
         * @param typeName name of the data type
         *
         * @return builder instance for fluent programming
         */
        public Builder typeName(final String typeName) {
            this.typeName = typeName;
            return this;
        }

        /**
         * Get the JDBC data type.
         *
         * @return JDBC data type
         */
        public int getJdbcDataType() {
            return this.jdbcDataType;
        }

        /**
         * Get the name of the data type.
         *
         * @return data type name
         */
        public String getTypeName() {
            return this.getTypeName();
        }

        /**
         * Build a new instance of the {@link ColumnAdapterNotes}.
         *
         * @return new instance of {@link ColumnAdapterNotes}
         */
        public ColumnAdapterNotes build() {
            return new ColumnAdapterNotes(this);
        }
    }
}
