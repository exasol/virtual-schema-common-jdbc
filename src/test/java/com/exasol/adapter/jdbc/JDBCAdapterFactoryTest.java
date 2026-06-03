package com.exasol.adapter.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterFactory;
import com.exasol.adapter.dialects.derby.DerbySqlDialectFactory;

class JDBCAdapterFactoryTest {
    @AfterEach
    void resetCloseFlag() {
        DerbySqlDialectFactory.resetClosedFlag();
    }

    @Test
    void getAdapterName() {
        try (final AdapterFactory factory = new JDBCAdapterFactory()) {
            assertThat(factory.getAdapterName(), equalTo("DERBY JDBC Adapter"));
        }
    }

    @Test
    void close() {
        final JDBCAdapterFactory factory = new JDBCAdapterFactory();

        factory.close();

        assertThat(DerbySqlDialectFactory.wasClosed(), equalTo(true));
    }
}
