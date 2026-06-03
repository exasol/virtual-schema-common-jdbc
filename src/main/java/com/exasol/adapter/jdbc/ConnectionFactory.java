package com.exasol.adapter.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for factories creating custom JDBC connections.
 */
public interface ConnectionFactory extends AutoCloseable {
    /**
     * Create a JDBC connection to the remote data source.
     *
     * @return JDBC connection to remote data source
     * @throws SQLException if the connection to the remote source could not be established
     */
    Connection getConnection() throws SQLException;

    @Override
    default void close() {
        // default implementation does nothing, override if the factory needs to release resources
    }
}
