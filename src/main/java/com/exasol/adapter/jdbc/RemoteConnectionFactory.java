package com.exasol.adapter.jdbc;

import static com.exasol.adapter.AdapterProperties.CONNECTION_NAME_PROPERTY;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import com.exasol.*;
import com.exasol.adapter.AdapterProperties;
import com.exasol.auth.kerberos.KerberosConfigurationCreator;
import com.exasol.errorreporting.ExaError;

/**
 * Factory that produces JDBC connections to remote data sources.
 */
public final class RemoteConnectionFactory implements ConnectionFactory {
    private static final Logger LOGGER = Logger.getLogger(RemoteConnectionFactory.class.getName());
    private final ExaMetadata exaMetadata;
    private final AdapterProperties properties;
    private Connection cachedConnection = null;

    public RemoteConnectionFactory(final ExaMetadata exaMetadata, final AdapterProperties properties) {
        this.exaMetadata = exaMetadata;
        this.properties = properties;
    }

    /**
     * Create a JDBC connection to the remote data source.
     *
     * @return JDBC connection to remote data source
     * @throws SQLException if the connection to the remote source could not be established
     */
    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (this.cachedConnection == null) {
            final String connectionName = this.properties.getConnectionName();
            if ((connectionName != null) && !connectionName.isEmpty()) {
                this.cachedConnection = createConnection(connectionName, this.exaMetadata);
            } else {
                throw new RemoteConnectionException(ExaError.messageBuilder("E-VS-COM-JDBC-28")
                        .message("{{connectionNameProperty}} property is missing or empty.")
                        .parameter("connectionNameProperty", CONNECTION_NAME_PROPERTY)
                        .mitigation("Please, check the property and try to connect again.").toString());
            }
        }
        return this.cachedConnection;
    }

    private Connection createConnection(final String connectionName, final ExaMetadata exaMetadata)
            throws SQLException {
        try {
            final ExaConnectionInformation exaConnection = exaMetadata.getConnection(connectionName);
            final String password = exaConnection.getPassword();
            final String username = exaConnection.getUser();
            final String address = exaConnection.getAddress();
            if (KerberosConfigurationCreator.isKerberosAuthentication(password)) {
                return establishConnectionWithKerberos(password, username, address);
            } else {
                return establishConnectionWithRegularCredentials(password, username, address);
            }
        } catch (final ExaConnectionAccessException exception) {
            throw new RemoteConnectionException(ExaError.messageBuilder("E-VS-COM-JDBC-29")
                    .message("Could not access the connection information of connection {{connectionName}}.")
                    .parameter("connectionName", connectionName).toString(), exception);
        }
    }

    private Connection establishConnectionWithKerberos(final String password, final String username,
            final String address) throws SQLException {
        logConnectionAttemptWithKerberos(address, username);
        final Properties jdbcProperties = new Properties();
        jdbcProperties.put("user", username);
        jdbcProperties.put("password", password);
        final KerberosConfigurationCreator kerberosConfigurationCreator = new KerberosConfigurationCreator();
        kerberosConfigurationCreator.writeKerberosConfigurationFiles(username, password);
        final long start = System.currentTimeMillis();
        final Connection connection = DriverManager.getConnection(address, jdbcProperties);
        logRemoteDatabaseDetails(connection, System.currentTimeMillis() - start);
        return connection;
    }

    private void logConnectionAttemptWithKerberos(final String address, final String username) {
        LOGGER.fine(
                () -> "Connecting to \"" + address + "\" as user \"" + username + "\" using Kerberos authentication.");
    }

    protected void logRemoteDatabaseDetails(final Connection connection, final long connectionTime)
            throws SQLException {
        final String databaseProductName = connection.getMetaData().getDatabaseProductName();
        final String databaseProductVersion = connection.getMetaData().getDatabaseProductVersion();
        LOGGER.info(() -> "Connected to " + databaseProductName + " " + databaseProductVersion + " in " + connectionTime
                + " milliseconds.");
    }

    private Connection establishConnectionWithRegularCredentials(final String password, final String username,
            final String address) throws SQLException {
        logConnectionAttempt(address, username);
        final long start = System.currentTimeMillis();
        final Connection connection = DriverManager.getConnection(address, username, password);
        logRemoteDatabaseDetails(connection, System.currentTimeMillis() - start);
        return connection;
    }

    protected void logConnectionAttempt(final String address, final String username) {
        LOGGER.fine(
                () -> "Connecting to \"" + address + "\" as user \"" + username + "\" using password authentication.");
    }
}