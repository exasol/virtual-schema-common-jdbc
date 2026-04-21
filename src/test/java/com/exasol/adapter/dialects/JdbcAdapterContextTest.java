package com.exasol.adapter.dialects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.telemetry.TelemetryClient;

class JdbcAdapterContextTest {
    @Test
    void testBuilderStoresConfiguredValuesAndTransfersThemToBuiltContext() {
        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        final AdapterProperties properties = new AdapterProperties(Map.of("CONNECTION_NAME", "MY_CONNECTION"));
        final ExaMetadata metadata = mock(ExaMetadata.class);
        final TelemetryClient telemetryClient = mock(TelemetryClient.class);

        final JDBCAdapterContext.Builder builder = JDBCAdapterContext.builder()
                .connectionFactory(connectionFactory)
                .properties(properties)
                .metadata(metadata)
                .telemetryClient(telemetryClient);

        final JDBCAdapterContext context = builder.build();

        assertAll(
                () -> assertThat(context.getConnectionFactory(), sameInstance(connectionFactory)),
                () -> assertThat(context.getProperties(), sameInstance(properties)),
                () -> assertThat(context.getExaMetadata(), sameInstance(metadata)),
                () -> assertThat(context.getTelemetryClient(), sameInstance(telemetryClient)));
    }

    @Test
    void testBuilderAndContextDefaultToNullValues() {
        final JDBCAdapterContext.Builder builder = JDBCAdapterContext.builder();

        final JDBCAdapterContext context = builder.build();

        assertAll(
                () -> assertThat(context.getConnectionFactory(), nullValue()),
                () -> assertThat(context.getProperties(), nullValue()),
                () -> assertThat(context.getExaMetadata(), nullValue()),
                () -> assertThat(context.getTelemetryClient(), nullValue()));
    }
}
