package com.exasol.adapter.jdbc;

import static com.exasol.adapter.AdapterProperties.CONNECTION_NAME_PROPERTY;

import com.exasol.ExaConnectionInformation;
import com.exasol.adapter.AdapterProperties;
import com.exasol.errorreporting.ExaError;

/**
 * This class creates the connection definition part of <code>IMPORT</code> statements.
 *
 * @see <a href="https://docs.exasol.com/sql/import.htm">IMPORT (Exasol documentation)</a>
 */
public class BaseConnectionDefinitionBuilder implements ConnectionDefinitionBuilder {
    /**
     * Get the connection definition part of a push-down query.
     *
     * @param properties               user-defined adapter properties
     * @param exaConnectionInformation details of a named Exasol connection
     * @return credentials part of the push-down query
     */
    @Override
    public String buildConnectionDefinition(final AdapterProperties properties,
            final ExaConnectionInformation exaConnectionInformation) {
        if (properties.hasConnectionName()) {
            return "AT " + properties.getConnectionName();
        } else {
            throw new IllegalArgumentException(ExaError.messageBuilder("E-VS-COM-JDBC-23")
                    .message("Please, provide a mandatory property {{connectionNameProperty}}.")
                    .parameter("connectionNameProperty", CONNECTION_NAME_PROPERTY).toString());
        }
    }

    /**
     * Build a connection definition.
     *
     * @param connectionString connection string
     * @param username         username
     * @param password         password
     * @return connection definition string
     */
    protected String getConnectionDefinition(final String connectionString, final String username,
            final String password) {
        return "AT '" + connectionString.replace("'", "''") + "' USER '" + username.replace("'", "''")
                + "' IDENTIFIED BY '" + password.replace("'", "''") + "'";
    }
}