package com.exasol.adapter.jdbc;

import com.exasol.adapter.capabilities.*;

class CapabilitiesParser {

    private static final String SCALAR_FUNCTION_PREFIX = "FN_";
    private static final String PREDICATE_PREFIX = "FN_PRED_";
    private static final String AGGREGATE_FUNCTION_PREFIX = "FN_AGG_";
    private static final String LITERAL_PREFIX = "LITERAL_";

    private CapabilitiesParser() {
        // This utility class should not be instantiated
    }

    static Capabilities parseExcludedCapabilities(final String capabilitiesString) {
        final Capabilities.Builder builder = Capabilities.builder();

        for (String capability : capabilitiesString.split(",")) {
            capability = capability.trim();
            if (capability.isEmpty()) {
                continue;
            }
            if (capability.startsWith(LITERAL_PREFIX)) {
                builder.addLiteral(parseLiteralCapability(capability));
            } else if (capability.startsWith(AGGREGATE_FUNCTION_PREFIX)) {
                builder.addAggregateFunction(parseAggregateFunctionCapability(capability));
            } else if (capability.startsWith(PREDICATE_PREFIX)) {
                builder.addPredicate(parsePredicateCapability(capability));
            } else if (capability.startsWith(SCALAR_FUNCTION_PREFIX)) {
                builder.addScalarFunction(parseScalarFunctionCapability(capability));
            } else {
                builder.addMain(parseMainCapability(capability));
            }
        }
        return builder.build();
    }

    private static MainCapability parseMainCapability(final String capability) {
        return MainCapability.valueOf(capability);
    }

    private static ScalarFunctionCapability parseScalarFunctionCapability(final String capability) {
        final String scalarFunctionCapabilities = capability.replaceFirst(SCALAR_FUNCTION_PREFIX, "");
        return ScalarFunctionCapability.valueOf(scalarFunctionCapabilities);
    }

    private static PredicateCapability parsePredicateCapability(final String capability) {
        final String predicateCapabilities = capability.replaceFirst(PREDICATE_PREFIX, "");
        return PredicateCapability.valueOf(predicateCapabilities);
    }

    private static AggregateFunctionCapability parseAggregateFunctionCapability(final String capability) {
        final String aggregateFunctionCap = capability.replaceFirst(AGGREGATE_FUNCTION_PREFIX, "");
        return AggregateFunctionCapability.valueOf(aggregateFunctionCap);
    }

    private static LiteralCapability parseLiteralCapability(final String capability) {
        final String literalCapabilities = capability.replaceFirst(LITERAL_PREFIX, "");
        return LiteralCapability.valueOf(literalCapabilities);
    }
}
