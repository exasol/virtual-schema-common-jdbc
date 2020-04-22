package com.exasol.adapter.jdbc;

import static com.exasol.adapter.AdapterProperties.CONNECTION_NAME_PROPERTY;
import static com.exasol.adapter.AdapterProperties.SQL_DIALECT_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.exasol.ExaMetadata;
import com.exasol.adapter.*;

class JdbcAdapterIT {
    @Test
    public void testRegisteredDialects() throws AdapterException {
        final String rawRequest = "{\n" //
                + "    \"type\" : \"getCapabilities\",\n" //
                + "    \"schemaMetadataInfo\" :\n" //
                + "    {\n" //
                + "        \"name\" : \"foo\",\n" //
                + "        \"properties\" :\n" //
                + "        {\n" //
                + "            \"" + SQL_DIALECT_PROPERTY + "\" : \"DERBY\"\n," //
                + "            \"" + CONNECTION_NAME_PROPERTY + "\" : \"derby_connection\"\n" //
                + "        }\n" //
                + "    }\n" //
                + "}";
        final ExaMetadata exaMetadata = Mockito.mock(ExaMetadata.class);
        RequestDispatcher.adapterCall(exaMetadata, rawRequest);
        final List<AdapterFactory> registeredFactories = AdapterRegistry.getInstance().getRegisteredAdapterFactories();
        assertThat(registeredFactories, hasItem(instanceOf(JdbcAdapterFactory.class)));
    }
}