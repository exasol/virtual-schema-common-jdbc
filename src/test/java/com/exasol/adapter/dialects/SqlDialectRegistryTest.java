package com.exasol.adapter.dialects;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exasol.adapter.dialects.derby.DerbySqlDialect;
import com.exasol.adapter.dialects.derby.DerbySqlDialectFactory;

class SqlDialectRegistryTest {
    final SqlDialectRegistry registry = SqlDialectRegistry.getInstance();

    @Test
    void testGetInstance() {
        final SqlDialectRegistry dialects = SqlDialectRegistry.getInstance();
        assertThat(dialects, instanceOf(SqlDialectRegistry.class));
    }

    @Test
    void testGetInstanceTwiceYieldsSameInstance() {
        assertThat(SqlDialectRegistry.getInstance(), sameInstance(SqlDialectRegistry.getInstance()));
    }

    @Test
    void testLoadSqlDialectFactories() {
        assertThat(this.registry.getRegisteredAdapterFactories(), hasItem(instanceOf(DerbySqlDialectFactory.class)));
    }

    @Test
    void testIsSupported() {
        assertThat(this.registry.hasDialectWithName("DERBY"), is(true));
    }

    @Test
    void testIsNotSupported() {
        assertThat(SqlDialectRegistry.getInstance().hasDialectWithName("Unknown Dialect"), is(false));
    }

    @Test
    void testGetSqlDialectForName() {
        assertThat(SqlDialectRegistry.getInstance().getDialectForName("DERBY", null, null),
                instanceOf(DerbySqlDialect.class));
    }

    @Test
    void testListRegisteredDialects() {
        final String dialectNames = this.registry.listRegisteredSqlDialectNames();
        assertThat(dialectNames, equalTo("\"DERBY\", \"DUMMYDIALECT\""));
    }

    @Test
    void testGetRegisteredDialectNames() {
        final Set<String> dialectNames = this.registry.getRegisteredAdapterNames();
        assertThat(dialectNames, containsInAnyOrder("DERBY", "DUMMYDIALECT"));
    }

    @Test
    void testGetSqlDialectClassForNameThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlDialectRegistry.getInstance().getDialectForName("DOESNOTEXIST", null, null));
    }
}