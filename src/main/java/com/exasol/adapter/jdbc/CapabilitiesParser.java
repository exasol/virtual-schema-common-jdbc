package com.exasol.adapter.jdbc;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;

import com.exasol.adapter.capabilities.*;
import com.exasol.errorreporting.ExaError;

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
        return parseCapability(capability, capability, MainCapability.class, "", "main capability");
    }

    private static ScalarFunctionCapability parseScalarFunctionCapability(final String capability) {
        final String scalarFunctionCapabilities = capability.replaceFirst(SCALAR_FUNCTION_PREFIX, "");
        return parseCapability(capability, scalarFunctionCapabilities, ScalarFunctionCapability.class, SCALAR_FUNCTION_PREFIX, "scalar function capability");
    }

    private static PredicateCapability parsePredicateCapability(final String capability) {
        final String predicateCapabilities = capability.replaceFirst(PREDICATE_PREFIX, "");
        return parseCapability(capability, predicateCapabilities, PredicateCapability.class, PREDICATE_PREFIX, "predicate capability");
    }

    private static AggregateFunctionCapability parseAggregateFunctionCapability(final String capability) {
        final String aggregateFunctionCap = capability.replaceFirst(AGGREGATE_FUNCTION_PREFIX, "");
        return parseCapability(capability, aggregateFunctionCap, AggregateFunctionCapability.class, AGGREGATE_FUNCTION_PREFIX, "aggregate function capability");
    }

    private static LiteralCapability parseLiteralCapability(final String capability) {
        final String literalCapabilities = capability.replaceFirst(LITERAL_PREFIX, "");
        return parseCapability(capability, literalCapabilities, LiteralCapability.class, LITERAL_PREFIX, "literal capability");
    }

    private static <T extends Enum<T>> T parseCapability(final String originalCapability, final String enumValue,
            final Class<T> capabilityClass, final String capabilityPrefix, final String capabilityType) {
        try {
            return Enum.valueOf(capabilityClass, enumValue);
        } catch (final IllegalArgumentException exception) {
            throw new IllegalArgumentException(ExaError.messageBuilder("E-VSCJDBC-48")
                    .message("Unsupported capability {{capability}} for {{capability type|uq}}.")
                    .mitigation("Use one of the available capabilities: {{available capabilities|uq}}")
                    .parameter("capability", originalCapability)
                    .parameter("capability type", capabilityType)
                    .parameter("available capabilities", getAvailableCapabilities(capabilityClass, capabilityPrefix))
                    .toString(), exception);
        }
    }

    private static <T extends Enum<T>> String getAvailableCapabilities(final Class<T> capabilityClass,
            final String capabilityPrefix) {
        return Arrays.stream(capabilityClass.getEnumConstants())
                .map(capability -> capabilityPrefix + capability.name())
                .sorted()
                .collect(joining(", ", "[", "]"));
    }
}
