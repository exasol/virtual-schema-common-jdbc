package com.exasol.adapter.jdbc;

import static com.exasol.adapter.AdapterProperties.CONNECTION_NAME_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.util.Optional;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.exasol.ExaMetadata;
import com.exasol.adapter.*;

/**
 * This class exists to integration test the JDBC adapter.
 */
class JDBCAdapterIT {
    @Test
    void testRegisteredAdapterFactories() {
        final ServiceLoader<AdapterFactory> adapterFactoryLoader = ServiceLoader.load(AdapterFactory.class);
        final Optional<AdapterFactory> adapterFactory = adapterFactoryLoader.findFirst();
        assertThat(adapterFactory.orElseThrow(), instanceOf(JDBCAdapterFactory.class));
    }

    @Test
    void testAdapterCall() throws AdapterException {
        final String rawRequest = "{\n"
                + "    \"type\" : \"getCapabilities\",\n"
                + "    \"schemaMetadataInfo\" :\n"
                + "    {\n"
                + "        \"name\" : \"foo\",\n"
                + "        \"properties\" :\n"
                + "        {\n"
                + "            \"" + CONNECTION_NAME_PROPERTY + "\" : \"DERBY_CONNECTION\"\n"
                + "        }\n"
                + "    }\n"
                + "}";
        final ExaMetadata exaMetadata = Mockito.mock(ExaMetadata.class);
        final String result = RequestDispatcher.adapterCall(exaMetadata, rawRequest);
        assertThat(result, equalTo(
                "{\"type\":\"getCapabilities\",\"capabilities\":[\"ORDER_BY_EXPRESSION\",\"FN_ADD\",\"FN_PRED_AND\",\"FN_AGG_COUNT_STAR\",\"LITERAL_NULL\"]}"));
    }
}
