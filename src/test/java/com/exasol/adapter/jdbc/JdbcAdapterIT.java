package com.exasol.adapter.jdbc;

import static com.exasol.adapter.AdapterProperties.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

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
                + "            \"" + CONNECTION_STRING_PROPERTY + "\" : \"jdbc:derby:memory:test;create=true;\"\n," //
                + "            \"" + USERNAME_PROPERTY + "\" : \"\"\n," //
                + "            \"" + PASSWORD_PROPERTY + "\" : \"\"\n" //
                + "        }\n" //
                + "    }\n" //
                + "}";
        final ExaMetadata exaMetadata = Mockito.mock(ExaMetadata.class);
        RequestDispatcher.adapterCall(exaMetadata, rawRequest);
        final List<AdapterFactory> registeredFactories = AdapterRegistry.getInstance().getRegisteredAdapterFactories();
        assertThat(registeredFactories, hasItem(instanceOf(JdbcAdapterFactory.class)));
    }
}