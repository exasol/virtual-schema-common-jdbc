package com.exasol.adapter.dialects;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.ConnectionFactory;

/**
 * This class implements a registry for supported SQL dialects.
 */
public final class SqlDialectRegistry {
    private static final Logger LOGGER = Logger.getLogger(SqlDialectRegistry.class.getName());
    private static SqlDialectRegistry instance;
    private final Map<String, SqlDialectFactory> registeredFactories = new HashMap<>();

    /**
     * Get the singleton instance of the {@link SqlDialectRegistry}.
     *
     * @return singleton instance
     */
    public static final synchronized SqlDialectRegistry getInstance() {
        if (instance == null) {
            LOGGER.finer(() -> "Instanciating SQL dialect registry and loading adapter factories.");
            instance = new SqlDialectRegistry();
            instance.loadSqlDialectFactories();
        }
        return instance;
    }

    private void loadSqlDialectFactories() {
        final ServiceLoader<SqlDialectFactory> serviceLoader = ServiceLoader.load(SqlDialectFactory.class);
        final Iterator<SqlDialectFactory> factories = serviceLoader.iterator();
        while (factories.hasNext()) {
            final SqlDialectFactory factory = factories.next();
            registerSqlDialectFactory(factory);
        }
        LOGGER.fine(() -> "Registered SQL dialects: " + listRegisteredSqlDialectNames());
    }

    /**
     * Register a factory for an {@link SqlDialect}.
     *
     * @param factory factory that creates an {@link SqlDialect}
     */
    private void registerSqlDialectFactory(final SqlDialectFactory factory) {
        this.registeredFactories.put(factory.getSqlDialectName(), factory);
    }

    /**
     * Get a list of all currently registered SQL dialect adapters.
     *
     * @return list of dialect factories
     */
    public List<SqlDialectFactory> getRegisteredAdapterFactories() {
        return new ArrayList<>(this.registeredFactories.values());
    }

    /**
     * Get a list of the name of all registered SQL dialect adapters.
     *
     * @return list of dialect names
     */
    public Set<String> getRegisteredAdapterNames() {
        return this.registeredFactories.values() //
                .stream() //
                .map(SqlDialectFactory::getSqlDialectName) //
                .collect(Collectors.toSet());
    }

    /**
     * Get the SQL dialect registered under the given name.
     *
     * @param name              name of the SQL dialect
     * @param connectionFactory factory for JDBC connection to the remote data source
     * @param properties        user-defined adapter properties
     * @return dialect instance
     */
    public SqlDialect getDialectForName(final String name, final ConnectionFactory connectionFactory,
            final AdapterProperties properties) {
        if (hasDialectWithName(name)) {
            final SqlDialectFactory factory = this.registeredFactories.get(name);
            LOGGER.config(() -> "Loading SQL dialect: " + factory.getSqlDialectName() + " dialect adapter "
                    + factory.getSqlDialectVersion());
            return factory.createSqlDialect(connectionFactory, properties);
        } else {
            throw new IllegalArgumentException("Unknown SQL dialect \"" + name + "\" requested. " + describe());
        }
    }

    /**
     * Check if an SQL dialect with the given name is registered.
     *
     * @param name adapter name to be searched for
     * @return <code>true</code> if an adapter is registered under that name
     */
    public boolean hasDialectWithName(final String name) {
        return this.registeredFactories.containsKey(name);
    }

    /**
     * Remove all registered adapters from the registry.
     */
    public void clear() {
        this.registeredFactories.clear();
    }

    /**
     * Describe the contents of the registry.
     *
     * @return description
     */
    public String describe() {
        return "Currently registered SQL dialect factories: " + listRegisteredSqlDialectNames();
    }

    /**
     * List the names of all registered SQL dialects.
     *
     * @return comma-separated string containing list of SQL dialect names
     */
    public String listRegisteredSqlDialectNames() {
        final String dialectNamesAsString = this.registeredFactories.keySet() //
                .stream() //
                .sorted().map(name -> "\"" + name + "\"") //
                .collect(Collectors.joining(", "));
        return dialectNamesAsString.isEmpty() ? "none" : dialectNamesAsString;
    }
}
