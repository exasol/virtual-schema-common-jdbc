package com.exasol.adapter.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.exasol.adapter.capabilities.*;

class CapabilitiesParserTest {
    @Test
    void parsesMainCapability() {
        final Capabilities capabilities = CapabilitiesParser.parseExcludedCapabilities("ORDER_BY_EXPRESSION");

        assertThat(capabilities.getMainCapabilities(), contains(MainCapability.ORDER_BY_EXPRESSION));
    }

    @Test
    void parsesLiteralCapability() {
        final Capabilities capabilities = CapabilitiesParser.parseExcludedCapabilities("LITERAL_NULL");

        assertThat(capabilities.getLiteralCapabilities(), contains(LiteralCapability.NULL));
    }

    @Test
    void parsesAggregateFunctionCapability() {
        final Capabilities capabilities = CapabilitiesParser.parseExcludedCapabilities("FN_AGG_COUNT");

        assertThat(capabilities.getAggregateFunctionCapabilities(), contains(AggregateFunctionCapability.COUNT));
    }

    @Test
    void parsesPredicateCapability() {
        final Capabilities capabilities = CapabilitiesParser.parseExcludedCapabilities("FN_PRED_AND");

        assertThat(capabilities.getPredicateCapabilities(), contains(PredicateCapability.AND));
    }

    @Test
    void parsesScalarFunctionCapability() {
        final Capabilities capabilities = CapabilitiesParser.parseExcludedCapabilities("FN_ADD");

        assertThat(capabilities.getScalarFunctionCapabilities(), contains(ScalarFunctionCapability.ADD));
    }

    @Test
    void skipsEmptyEntries() {
        final Capabilities capabilities = CapabilitiesParser.parseExcludedCapabilities(" , , ");

        assertThat(capabilities.getMainCapabilities(), empty());
        assertThat(capabilities.getLiteralCapabilities(), empty());
        assertThat(capabilities.getAggregateFunctionCapabilities(), empty());
        assertThat(capabilities.getPredicateCapabilities(), empty());
        assertThat(capabilities.getScalarFunctionCapabilities(), empty());
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "INVALID_MAIN_CAPABILITY|E-VSCJDBC-48: Unsupported capability 'INVALID_MAIN_CAPABILITY' for main capability. Use one of the available capabilities: [AGGREGATE_GROUP_BY_COLUMN",
            "LITERAL_INVALID|E-VSCJDBC-48: Unsupported capability 'LITERAL_INVALID' for literal capability. Use one of the available capabilities: [LITERAL_BOOL",
            "FN_AGG_INVALID|E-VSCJDBC-48: Unsupported capability 'FN_AGG_INVALID' for aggregate function capability. Use one of the available capabilities: [FN_AGG_APPROXIMATE_COUNT_DISTINCT,",
            "FN_PRED_INVALID|E-VSCJDBC-48: Unsupported capability 'FN_PRED_INVALID' for predicate capability. Use one of the available capabilities: [FN_PRED_AND",
            "FN_INVALID|E-VSCJDBC-48: Unsupported capability 'FN_INVALID' for scalar function capability. Use one of the available capabilities: [FN_ABS",
            // Lower case capability names should also be rejected
            "order_by_expression|E-VSCJDBC-48: Unsupported capability 'order_by_expression' for main capability. Use one of the available capabilities: [AGGREGATE_GROUP_BY_COLUMN",
            "literal_null|E-VSCJDBC-48: Unsupported capability 'literal_null' for main capability. Use one of the available capabilities: [AGGREGATE_GROUP_BY_COLUMN",
            "fn_agg_count|E-VSCJDBC-48: Unsupported capability 'fn_agg_count' for main capability. Use one of the available capabilities: [AGGREGATE_GROUP_BY_COLUMN",
            "fn_pred_and|E-VSCJDBC-48: Unsupported capability 'fn_pred_and' for main capability. Use one of the available capabilities: [AGGREGATE_GROUP_BY_COLUMN",
            "fn_add|E-VSCJDBC-48: Unsupported capability 'fn_add' for main capability. Use one of the available capabilities: [AGGREGATE_GROUP_BY_COLUMN",
    })
    void throwsExceptionForInvalidCapabilityNames(final String capability, final String expectedMessagePrefix) {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CapabilitiesParser.parseExcludedCapabilities(capability));

        assertThat(exception.getMessage(), startsWith(expectedMessagePrefix));
    }
}
