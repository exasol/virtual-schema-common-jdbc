package com.exasol.adapter.properties;

import java.util.EnumSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.exasol.adapter.AdapterProperties;
import com.exasol.errorreporting.ExaError;

/**
 * This class represents a special Adapter Property for JDBC-based Virtual Schemas controlling the strategy for
 * determining data types for {@code IMPORT} statement.
 */
public class DataTypeDetection {

    /** Name of the Adapter Property to be passed to {@code CREATE VIRTUAL SCHEMA} */
    public static final String STRATEGY_PROPERTY = "IMPORT_DATA_TYPES";
    static final Strategy DEFAULT_STRATEGY = Strategy.EXASOL_CALCULATED;

    /**
     * @return validator for the property controlling the strategy for data type detection.
     */
    public static PropertyValidator getValidator() {
        return PropertyValidator.optional(STRATEGY_PROPERTY, DataTypeDetection::validatePropertyValue);
    }

    private static void validatePropertyValue(final String value) throws PropertyValidationException {
        if (value.equals(Strategy.FROM_RESULT_SET.toString())) {
            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-47")
                    .message("Property `IMPORT_DATA_TYPES` value 'FROM_RESULT_SET' is no longer supported.")
                    .mitigation(
                            "Please remove the `IMPORT_DATA_TYPES` property from the virtual schema so the default value 'EXASOL_CALCULATED' is used.")
                    .toString());
        }
        if (!strategies(Collectors.toSet()).contains(value)) {
            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-41")
                    .message("Invalid value {{value}} for property {{property}}.", value, STRATEGY_PROPERTY)
                    .mitigation("Choose one of: {{availableValues|uq}}.", strategies(Collectors.joining(", ")))
                    .toString());
        }
    }

    /**
     * @param properties Adapter Properties passed to {@code CREATE VIRTUAL SCHEMA}
     * @return new instance of {@link DataTypeDetection} based on the properties
     */
    public static DataTypeDetection from(final AdapterProperties properties) {
        return new DataTypeDetection(getStrategy(properties));
    }

    /**
     * @param <T>       Type of strategy
     * @param collector collector to collect the strategies
     * @return result of collecting the strategies
     */
    public static <T> T strategies(final Collector<CharSequence, ?, T> collector) {
        return EnumSet.allOf(Strategy.class).stream().map(Enum::toString).collect(collector);
    }

    private static Strategy getStrategy(final AdapterProperties properties) {
        if (properties.containsKey(STRATEGY_PROPERTY)) {
            if (Strategy.FROM_RESULT_SET.name().equals(properties.get(STRATEGY_PROPERTY))) {
                return Strategy.FROM_RESULT_SET;
            } else {
                return DEFAULT_STRATEGY;
            }
        } else {
            return DEFAULT_STRATEGY;
        }
    }

    /**
     * Strategies for data type detection
     */
    public enum Strategy {
        /** Infer data types for {@code IMPORT} statement from values of the result set */
        FROM_RESULT_SET,
        /** Let Exasol database calculate the data types for {@code IMPORT} statement based on metadata of connection */
        EXASOL_CALCULATED;
    }

    private final Strategy strategy;

    DataTypeDetection(final Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * @return strategy
     */
    public Strategy getStrategy() {
        return this.strategy;
    }
}
