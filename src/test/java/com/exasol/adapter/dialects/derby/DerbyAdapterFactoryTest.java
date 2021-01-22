package com.exasol.adapter.dialects.derby;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

class DerbyAdapterFactoryTest {
    private static final DerbyAdapterFactory FACTORY = new DerbyAdapterFactory();

    @Test
    void testGetSupportedAdapterNames() throws Exception {
        assertThat(DerbyAdapterFactoryTest.FACTORY.getSupportedAdapterNames(), contains("DERBY"));
    }

    @Test
    void testGetAdapterName() throws Exception {
        assertThat(DerbyAdapterFactoryTest.FACTORY.getAdapterName(), equalTo("DERBY JDBC Adapter"));
    }

    @Test
    void testGetAdapterVersion() throws Exception {
        assertThat(DerbyAdapterFactoryTest.FACTORY.getAdapterVersion(), endsWith(" (DERBY 0.0.0)"));
    }
}