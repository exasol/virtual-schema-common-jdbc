package com.exasol.adapter.dialects;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.telemetry.TelemetryClient;

public class JDBCAdapterContext {
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

    public static Builder builder() {
        return new Builder();
    }

    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    public AdapterProperties getProperties() {
        return this.properties;
    }

    public ExaMetadata getExaMetadata() {
        return this.metadata;
    }

    public TelemetryClient getTelemetryClient() {
        return this.telemetryClient;
    }

    public static final class Builder {
        private ConnectionFactory connectionFactory;
        private AdapterProperties properties;
        private ExaMetadata metadata;
        private TelemetryClient telemetryClient;

        public Builder connectionFactory(final ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        public Builder properties(final AdapterProperties properties) {
            this.properties = properties;
            return this;
        }

        public Builder metadata(final ExaMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder telemetryClient(final TelemetryClient telemetryClient) {
            this.telemetryClient = telemetryClient;
            return this;
        }

        public ConnectionFactory getConnectionFactory() {
            return this.connectionFactory;
        }

        public AdapterProperties getProperties() {
            return this.properties;
        }

        public ExaMetadata getMetadata() {
            return this.metadata;
        }

        public TelemetryClient getTelemetryClient() {
            return this.telemetryClient;
        }

        public JDBCAdapterContext build() {
            return new JDBCAdapterContext(this);
        }
    }
}
