package com.exasol.adapter.dialects;

import java.util.EnumSet;
import java.util.stream.Collector;

import com.exasol.adapter.AdapterProperties;

/**
 * This class represents a special Adapter Property for JDBC-based Virtual Schemas controlling the strategy for
 * determining data types for {@code IMPORT} statement.
 */
public class DataTypeDetection {

    /** Name of the Adapter Property to be passed to {@code CREATE VIRTUAL SCHEMA} */
    public static final String STRATEGY_PROPERTY = "IMPORT_DATA_TYPES";

    /**
     * @param properties Adapter Properties passed to {@code CREATE VIRTUAL SCHEMA}
     * @return new instance of {@link DataTypeDetection} based on the properties
     */
    public static DataTypeDetection from(final AdapterProperties properties) {
        return new DataTypeDetection(getStrategy(properties));
    }

    static <T> T strategies(final Collector<CharSequence, ?, T> collector) {
        return EnumSet.allOf(Strategy.class).stream().map(Enum::toString).collect(collector);
    }

    private static Strategy getStrategy(final AdapterProperties properties) {
        if (properties.containsKey(STRATEGY_PROPERTY)) {
            if (Strategy.FROM_RESULT_SET.name().equals(properties.get(STRATEGY_PROPERTY))) {
                return Strategy.FROM_RESULT_SET;
            } else {
                return Strategy.EXASOL_CALCULATED;
            }
        } else {
            return Strategy.EXASOL_CALCULATED;
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
