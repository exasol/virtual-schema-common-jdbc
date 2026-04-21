package com.exasol.adapter.dialects;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.telemetry.TelemetryClient;

/**
 * Context object that bundles services and metadata required by a JDBC adapter.
 */
public final class JDBCAdapterContext {
    private final ConnectionFactory connectionFactory;
    private final AdapterProperties properties;
    private final ExaMetadata metadata;
    private final TelemetryClient telemetryClient;

    private JDBCAdapterContext(final Builder builder) {
        this.connectionFactory = builder.connectionFactory;
        this.properties = builder.properties;
        this.metadata = builder.metadata;
        this.telemetryClient = builder.telemetryClient;
    }

    /**
     * Create a builder for a {@link JDBCAdapterContext}.
     * 
     * @return new context builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the connection factory used by the adapter.
     * 
     * @return connection factory
     */
    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    /**
     * Get the adapter properties.
     * 
     * @return adapter properties
     */
    public AdapterProperties getProperties() {
        return this.properties;
    }

    /**
     * Get the Exasol metadata of the current adapter call.
     * 
     * @return Exasol metadata
     */
    public ExaMetadata getExaMetadata() {
        return this.metadata;
    }

    /**
     * Get the telemetry client used for emitting telemetry events.
     * 
     * @return telemetry client
     */
    public TelemetryClient getTelemetryClient() {
        return this.telemetryClient;
    }

    /**
     * Builder for {@link JDBCAdapterContext} instances.
     */
    public static final class Builder {
        private ConnectionFactory connectionFactory;
        private AdapterProperties properties;
        private ExaMetadata metadata;
        private TelemetryClient telemetryClient;

        /**
         * Create a new builder.
         */
        public Builder() {
            // intentionally left blank
        }

        /**
         * Set the connection factory used by the adapter.
         * 
         * @param connectionFactory connection factory
         * @return this builder
         */
        public Builder connectionFactory(final ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        /**
         * Set the adapter properties.
         * 
         * @param properties adapter properties
         * @return this builder
         */
        public Builder properties(final AdapterProperties properties) {
            this.properties = properties;
            return this;
        }

        /**
         * Set the Exasol metadata of the current adapter call.
         * 
         * @param metadata Exasol metadata
         * @return this builder
         */
        public Builder metadata(final ExaMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Set the telemetry client used for emitting telemetry events.
         * 
         * @param telemetryClient telemetry client
         * @return this builder
         */
        public Builder telemetryClient(final TelemetryClient telemetryClient) {
            this.telemetryClient = telemetryClient;
            return this;
        }

        /**
         * Build a {@link JDBCAdapterContext} from the configured values.
         * 
         * @return new adapter context
         */
        public JDBCAdapterContext build() {
            return new JDBCAdapterContext(this);
        }
    }
}
