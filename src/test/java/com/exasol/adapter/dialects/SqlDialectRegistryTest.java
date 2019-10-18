package com.exasol.adapter.dialects;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.exasol.adapter.dialects.derby.DerbySqlDialectFactory;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.dialects.dummy.DummySqlDialectFactory;

class SqlDialectRegistryTest {
    final SqlDialectRegistry registry = SqlDialectRegistry.getInstance();

    @AfterEach
    void afterEach() {
        this.registry.clear();
    }

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
        this.registry.loadSqlDialectFactories();
        assertThat(this.registry.getRegisteredAdapterFactories(), hasItem(instanceOf(DerbySqlDialectFactory.class)));
    }

    @Test
    void testIsSupported() {
        this.registry.registerSqlDialectFactory(new DummySqlDialectFactory());
        assertThat(this.registry.hasDialectWithName("DUMMYDIALECT"), is(true));
    }

    @Test
    void testIsNotSupported() {
        assertThat(SqlDialectRegistry.getInstance().hasDialectWithName("Unknown Dialect"), is(false));
    }

    @Test
    void testGetSqlDialectForName() {
        this.registry.registerSqlDialectFactory(new DummySqlDialectFactory());
        assertThat(SqlDialectRegistry.getInstance().getDialectForName("DUMMYDIALECT", null, null),
                instanceOf(DummySqlDialect.class));
    }

    @Test
    void testListRegisteredDialects() {
        this.registry.registerSqlDialectFactory(new DerbySqlDialectFactory());
        this.registry.registerSqlDialectFactory(new DummySqlDialectFactory());
        final String dialectNames = this.registry.listRegisteredSqlDialectNames();
        assertThat(dialectNames, equalTo("\"DERBY\", \"DUMMYDIALECT\""));
    }

    @Test
    void testGetSqlDialectClassForNameThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlDialectRegistry.getInstance().getDialectForName("DOESNOTEXIST", null, null));
    }
}