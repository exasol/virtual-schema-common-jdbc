package com.exasol.adapter.dialects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.sql.*;

class SqlGenerationVisitorTest {
    private static SqlGenerationVisitor sqlGenerationVisitor;

    @BeforeAll
    static void setUp() {
        final Map<String, String> rawProperties = new HashMap<>();
        final AdapterProperties adapterProperties = new AdapterProperties(rawProperties);
        final SqlDialect sqlDialect = new DummySqlDialect(null, adapterProperties);
        final SqlGenerationContext context = new SqlGenerationContext("", "TEXT_SCHEMA_NAME", false);
        sqlGenerationVisitor = new SqlGenerationVisitor(sqlDialect, context);
    }

    @Test
    void testVisitSqlFunctionScalarCast() throws AdapterException {
        final DataType dataType = DataType.createDecimal(18, 0);
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlLiteralDouble(20));
        final SqlFunctionScalarCast sqlFunctionScalarCast = new SqlFunctionScalarCast(dataType, arguments);
        assertThat(sqlGenerationVisitor.visit(sqlFunctionScalarCast), equalTo("CAST(20.0 AS DECIMAL(18, 0))"));
    }

    @Test
    void testVisitSqlFunctionScalarExtract() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlLiteralTimestamp("2019-02-12 12:07:00"));
        final SqlFunctionScalarExtract sqlFunctionScalarExtract = new SqlFunctionScalarExtract("SECOND", arguments);
        assertThat(sqlGenerationVisitor.visit(sqlFunctionScalarExtract),
                equalTo("EXTRACT(SECOND FROM TIMESTAMP '2019-02-12 12:07:00')"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggOnOverflowError() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()));
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.ERROR);
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(arguments, overflowBehavior)
                .distinct(true).build();
        assertThat(sqlGenerationVisitor.visit(listagg), equalTo("LISTAGG(DISTINCT \"a\" ON OVERFLOW ERROR)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggWithSeparator() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()));
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.ERROR);
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(arguments, overflowBehavior)
                .separator(", ").build();
        assertThat(sqlGenerationVisitor.visit(listagg), equalTo("LISTAGG(\"a\", ', ' ON OVERFLOW ERROR)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggOnOverflowTruncate() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()));
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.TRUNCATE);
        overflowBehavior.setTruncationType("WITH COUNT");
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(arguments, overflowBehavior)
                .build();
        assertThat(sqlGenerationVisitor.visit(listagg), equalTo("LISTAGG(\"a\" ON OVERFLOW TRUNCATE WITH COUNT)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggOnOverflowTruncateWithFilter() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()));
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.TRUNCATE);
        overflowBehavior.setTruncationType("WITHOUT COUNT");
        overflowBehavior.setTruncationFiller("filler");
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(arguments, overflowBehavior)
                .build();
        assertThat(sqlGenerationVisitor.visit(listagg),
                equalTo("LISTAGG(\"a\" ON OVERFLOW TRUNCATE 'filler' WITHOUT COUNT)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggWithOrderBy() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()));
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.ERROR);
        final List<SqlNode> expressions = new ArrayList<>();
        expressions.add(new SqlColumn(1, ColumnMetadata.builder().name("b").type(DataType.createBool()).build()));
        final SqlOrderBy orderBy = new SqlOrderBy(expressions, List.of(true), List.of(true));
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(arguments, overflowBehavior)
                .orderBy(orderBy).build();
        assertThat(sqlGenerationVisitor.visit(listagg),
                equalTo("LISTAGG(\"a\" ON OVERFLOW ERROR) WITHIN GROUP (ORDER BY \"b\" NULLS LAST)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggFull() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()));
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.TRUNCATE);
        overflowBehavior.setTruncationType("WITH COUNT");
        overflowBehavior.setTruncationFiller("filler");
        final List<SqlNode> expressions = new ArrayList<>();
        expressions.add(new SqlColumn(1, ColumnMetadata.builder().name("b").type(DataType.createBool()).build()));
        final SqlOrderBy orderBy = new SqlOrderBy(expressions, List.of(false), List.of(true));
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(arguments, overflowBehavior)
                .orderBy(orderBy).distinct(true).separator(", ").build();
        assertThat(sqlGenerationVisitor.visit(listagg), equalTo(
                "LISTAGG(DISTINCT \"a\", ', ' ON OVERFLOW TRUNCATE 'filler' WITH COUNT) WITHIN GROUP (ORDER BY \"b\" DESC)"));
    }

    @Test
    void testCountWithMultipleArguments() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()));
        arguments.add(new SqlColumn(2, ColumnMetadata.builder().name("b").type(DataType.createBool()).build()));
        final SqlFunctionAggregate count = new SqlFunctionAggregate(AggregateFunction.COUNT, arguments, false);
        assertThat(sqlGenerationVisitor.visit(count), equalTo("COUNT((\"a\", \"b\"))"));
    }

    @Test
    void testCountWithMultipleArgumentsAndDistinct() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()));
        arguments.add(new SqlColumn(2, ColumnMetadata.builder().name("b").type(DataType.createBool()).build()));
        final SqlFunctionAggregate count = new SqlFunctionAggregate(AggregateFunction.COUNT, arguments, true);
        assertThat(sqlGenerationVisitor.visit(count), equalTo("COUNT(DISTINCT (\"a\", \"b\"))"));
    }

    @Test
    void testCountAll() throws AdapterException {
        final SqlFunctionAggregate count = new SqlFunctionAggregate(AggregateFunction.COUNT, Collections.emptyList(),
                false);
        assertThat(sqlGenerationVisitor.visit(count), equalTo("COUNT(*)"));
    }

    @Test
    void testCountDistinct() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()));
        final SqlFunctionAggregate count = new SqlFunctionAggregate(AggregateFunction.COUNT, arguments, true);
        assertThat(sqlGenerationVisitor.visit(count), equalTo("COUNT(DISTINCT \"a\")"));
    }

    @Test
    void testVisitSqlFunctionScalarJsonValue() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlLiteralString("{\"a\": 1}"));
        arguments.add(new SqlLiteralString("$.a"));
        final SqlFunctionScalarJsonValue.Behavior emptyBehavior = new SqlFunctionScalarJsonValue.Behavior(
                SqlFunctionScalarJsonValue.BehaviorType.DEFAULT, Optional.of(new SqlLiteralString("*** error ***")));
        final SqlFunctionScalarJsonValue.Behavior errorBehavior = new SqlFunctionScalarJsonValue.Behavior(
                SqlFunctionScalarJsonValue.BehaviorType.DEFAULT, Optional.of(new SqlLiteralString("*** error ***")));
        final SqlFunctionScalarJsonValue sqlFunctionScalarJsonValue = new SqlFunctionScalarJsonValue(
                ScalarFunction.JSON_VALUE, arguments, DataType.createVarChar(1000, DataType.ExaCharset.UTF8),
                emptyBehavior, errorBehavior);
        assertThat(sqlGenerationVisitor.visit(sqlFunctionScalarJsonValue),
                equalTo("JSON_VALUE('{\"a\": 1}', '$.a' RETURNING VARCHAR(1000) UTF8 "
                        + "DEFAULT '*** error ***' ON EMPTY DEFAULT '*** error ***' ON ERROR)"));
    }

    @Test
    void testVisitSqlLimit() {
        final SqlLimit sqlLimit = new SqlLimit(5, 10);
        assertThat(sqlGenerationVisitor.visit(sqlLimit), equalTo("LIMIT 5 OFFSET 10"));
    }

    @Test
    void testVisitSqlPredicateIsJson() throws AdapterException {
        final SqlNode expressionMock = Mockito.mock(SqlNode.class);
        when(expressionMock.accept(sqlGenerationVisitor)).thenReturn("SELECT '{\"a\": 1}'");
        final SqlPredicateIsJson sqlPredicateIsJson = new SqlPredicateIsJson(expressionMock,
                AbstractSqlPredicateJson.TypeConstraints.OBJECT,
                AbstractSqlPredicateJson.KeyUniquenessConstraint.WITH_UNIQUE_KEYS);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsJson),
                equalTo("SELECT '{\"a\": 1}' IS JSON OBJECT WITH UNIQUE"));
    }

    @Test
    void testVisitSqlPredicateIsNotJson() throws AdapterException {
        final SqlNode expressionMock = Mockito.mock(SqlNode.class);
        when(expressionMock.accept(sqlGenerationVisitor)).thenReturn("SELECT '{\"a\": 1}'");
        final SqlPredicateIsNotJson sqlPredicateIsNotJson = new SqlPredicateIsNotJson(expressionMock,
                AbstractSqlPredicateJson.TypeConstraints.OBJECT,
                AbstractSqlPredicateJson.KeyUniquenessConstraint.WITH_UNIQUE_KEYS);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsNotJson),
                equalTo("SELECT '{\"a\": 1}' IS NOT JSON OBJECT WITH UNIQUE"));
    }

    @Test
    void testVisitSqlPredicateIsNull() throws AdapterException {
        final SqlPredicateIsNull sqlPredicateIsNull = new SqlPredicateIsNull(
                new SqlPredicateLess(new SqlLiteralExactnumeric(BigDecimal.ONE), new SqlLiteralNull()));
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsNull), equalTo("(1 < NULL) IS NULL"));
    }

    @Test
    void testVisitSqlPredicateIsNotNull() throws AdapterException {
        final SqlPredicateIsNotNull sqlPredicateIsNotNull = new SqlPredicateIsNotNull(
                new SqlPredicateLess(new SqlLiteralExactnumeric(BigDecimal.ONE), new SqlLiteralNull()));
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsNotNull), equalTo("(1 < NULL) IS NOT NULL"));
    }

    @Test
    void testVisitSqlLiteralDate() {
        final SqlLiteralDate sqlLiteralDate = new SqlLiteralDate("2015-12-01");
        assertThat(sqlGenerationVisitor.visit(sqlLiteralDate), equalTo("DATE '2015-12-01'"));
    }
}