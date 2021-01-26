package com.exasol.adapter.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterFactory;

class JDBCAdapterFactoryTest {
    @Test
    void getAdapterName() {
        final AdapterFactory factory = new JDBCAdapterFactory();
        assertThat(factory.getAdapterName(), equalTo("DERBY JDBC Adapter"));
    }
}