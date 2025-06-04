package com.exasol.adapter.dialects;

import static com.exasol.adapter.sql.SqlFunctionAggregateListagg.Behavior.TruncationType.WITHOUT_COUNT;
import static com.exasol.adapter.sql.SqlFunctionAggregateListagg.Behavior.TruncationType.WITH_COUNT;
import static com.exasol.adapter.sql.SqlFunctionScalarExtract.ExtractParameter.SECOND;
import static com.exasol.adapter.sql.SqlFunctionScalarExtract.ExtractParameter.YEAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.*;

import com.exasol.ExaMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.dummy.DummySqlDialect;
import com.exasol.adapter.dialects.rewriting.SqlGenerationContext;
import com.exasol.adapter.dialects.rewriting.SqlGenerationVisitor;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.metadata.*;
import com.exasol.adapter.sql.*;
import com.exasol.adapter.sql.SqlFunctionScalarJsonValue.Behavior;
import com.exasol.adapter.sql.SqlFunctionScalarJsonValue.BehaviorType;

class SqlGenerationVisitorTest {
    private static SqlGenerationVisitor sqlGenerationVisitor;
    private static AdapterProperties adapterProperties;

    @BeforeAll
    static void beforeAll() {
        final Map<String, String> rawProperties = new HashMap<>();
        adapterProperties = new AdapterProperties(rawProperties);
        final SqlDialect sqlDialect = new DummySqlDialect(null, adapterProperties, null);
        final SqlGenerationContext context = new SqlGenerationContext("", "TEXT_SCHEMA_NAME", false);
        sqlGenerationVisitor = new SqlGenerationVisitor(sqlDialect, context);
    }

    @Test
    void testVisitSqlFunctionScalarCast() throws AdapterException {
        final DataType dataType = DataType.createDecimal(18, 0);
        final SqlLiteralDouble argument = new SqlLiteralDouble(20);
        final SqlFunctionScalarCast sqlFunctionScalarCast = new SqlFunctionScalarCast(dataType, argument);
        assertThat(sqlGenerationVisitor.visit(sqlFunctionScalarCast), equalTo("CAST(2E1 AS DECIMAL(18, 0))"));
    }

    @Test
    void testVisitSqlFunctionScalarExtract() throws AdapterException {
        final SqlLiteralTimestamp argument = new SqlLiteralTimestamp("2019-02-12 12:07:00");
        final SqlFunctionScalarExtract sqlFunctionScalarExtract = new SqlFunctionScalarExtract(SECOND, argument);
        assertThat(sqlGenerationVisitor.visit(sqlFunctionScalarExtract),
                equalTo("EXTRACT(SECOND FROM TIMESTAMP '2019-02-12 12:07:00')"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggOnOverflowError() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("a").type(DataType.createBool()).build());
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.ERROR);
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(argument, overflowBehavior)
                .distinct(true).build();
        assertThat(sqlGenerationVisitor.visit(listagg), equalTo("LISTAGG(DISTINCT \"a\" ON OVERFLOW ERROR)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggWithSeparator() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("a").type(DataType.createBool()).build());
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.ERROR);
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(argument, overflowBehavior)
                .separator(new SqlLiteralString(", ")).build();
        assertThat(sqlGenerationVisitor.visit(listagg), equalTo("LISTAGG(\"a\", ', ' ON OVERFLOW ERROR)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggOnOverflowTruncate() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("a").type(DataType.createBool()).build());
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.TRUNCATE);
        overflowBehavior.setTruncationType(WITH_COUNT);
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(argument, overflowBehavior)
                .build();
        assertThat(sqlGenerationVisitor.visit(listagg), equalTo("LISTAGG(\"a\" ON OVERFLOW TRUNCATE WITH COUNT)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggOnOverflowTruncateWithFilter() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("a").type(DataType.createBool()).build());
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.TRUNCATE);
        overflowBehavior.setTruncationType(WITHOUT_COUNT);
        overflowBehavior.setTruncationFiller(new SqlLiteralString("filler"));
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(argument, overflowBehavior)
                .build();
        assertThat(sqlGenerationVisitor.visit(listagg),
                equalTo("LISTAGG(\"a\" ON OVERFLOW TRUNCATE 'filler' WITHOUT COUNT)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggWithOrderBy() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("a").type(DataType.createBool()).build());
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.ERROR);
        final List<SqlNode> expressions = new ArrayList<>();
        expressions.add(new SqlColumn(1, ColumnMetadata.builder().name("b").type(DataType.createBool()).build()));
        final SqlOrderBy orderBy = new SqlOrderBy(expressions, List.of(true), List.of(true));
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(argument, overflowBehavior)
                .orderBy(orderBy).build();
        assertThat(sqlGenerationVisitor.visit(listagg),
                equalTo("LISTAGG(\"a\" ON OVERFLOW ERROR) WITHIN GROUP (ORDER BY \"b\" NULLS LAST)"));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggFull() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("a").type(DataType.createBool()).build());
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.TRUNCATE);
        overflowBehavior.setTruncationType(WITH_COUNT);
        overflowBehavior.setTruncationFiller(new SqlLiteralString("filler"));
        final List<SqlNode> expressions = new ArrayList<>();
        expressions.add(new SqlColumn(1, ColumnMetadata.builder().name("b").type(DataType.createBool()).build()));
        final SqlOrderBy orderBy = new SqlOrderBy(expressions, List.of(false), List.of(true));
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(argument, overflowBehavior)
                .orderBy(orderBy).distinct(true).separator(new SqlLiteralString(", ")).build();
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
    void testVisitSqlLiteralBoolTrue() {
        final SqlLiteralBool sqlLiteralBool = new SqlLiteralBool(true);
        assertThat(sqlGenerationVisitor.visit(sqlLiteralBool), equalTo("true"));
    }

    @Test
    void testVisitSqlLiteralBoolFalse() {
        final SqlLiteralBool sqlLiteralBool = new SqlLiteralBool(false);
        assertThat(sqlGenerationVisitor.visit(sqlLiteralBool), equalTo("false"));
    }

    @Test
    void testVisitSqlPredicateIsJson() throws AdapterException {
        final SqlNode expression = new SqlLiteralString("{\"a\": 1}");
        final SqlPredicateIsJson sqlPredicateIsJson = new SqlPredicateIsJson(expression,
                AbstractSqlPredicateJson.TypeConstraints.OBJECT,
                AbstractSqlPredicateJson.KeyUniquenessConstraint.WITH_UNIQUE_KEYS);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsJson), equalTo("'{\"a\": 1}' IS JSON OBJECT WITH UNIQUE"));
    }

    @Test
    void testVisitSqlPredicateIsNotJson() throws AdapterException {
        final SqlNode expression = new SqlLiteralString("{\"a\": 1}");
        final SqlPredicateIsNotJson sqlPredicateIsNotJson = new SqlPredicateIsNotJson(expression,
                AbstractSqlPredicateJson.TypeConstraints.OBJECT,
                AbstractSqlPredicateJson.KeyUniquenessConstraint.WITH_UNIQUE_KEYS);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsNotJson),
                equalTo("'{\"a\": 1}' IS NOT JSON OBJECT WITH UNIQUE"));
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

    @ParameterizedTest
    @CsvSource({ "1.0, 1E0", //
            "1.234, 1.234E0", //
            "345600000000, 3.456E11", //
            "0.0000000000000000009236, 9.236E-19", //
            "0.00098, 9.8E-4", //
            "1.2345000000000000e+02, 1.2345E2" //
    })
    void testVisitSqlLiteralDouble(final double input, final String expected) {
        final SqlLiteralDouble sqlLiteralDouble = new SqlLiteralDouble(input);
        assertThat(sqlGenerationVisitor.visit(sqlLiteralDouble), equalTo(expected));
    }

    @ParameterizedTest
    @CsvSource({ "1E-35, 0.00000000000000000000000000000000001", //
            "1E35, 100000000000000000000000000000000000", //
            "12345, 12345", //
            "3.456e11, 345600000000", //
            "9.8e-4, 0.00098", //
            "9.236E-19, 0.0000000000000000009236", //
            "123.45, 123.45", //
            "12345, 12345" //
    })
    void testVisitSqlLiteralExactnumeric(final BigDecimal input, final String expected) {
        final SqlLiteralExactnumeric sqlLiteralExactnumeric = new SqlLiteralExactnumeric(input);
        assertThat(sqlGenerationVisitor.visit(sqlLiteralExactnumeric), equalTo(expected));
    }

    @Test
    void testVisitSqlLiteralNull() {
        final SqlLiteralNull sqlLiteralNull = new SqlLiteralNull();
        assertThat(sqlGenerationVisitor.visit(sqlLiteralNull), equalTo("NULL"));
    }

    // Quoting tests
    @Test
    void testSelectQuoting() throws AdapterException {
        final SqlNode from = new SqlTable("\" t1 '", new TableMetadata("t", "", Collections.emptyList(), ""));
        final SqlNode column = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlNode whereRight = new SqlLiteralString("\" right '");
        final SqlNode where = new SqlPredicateLess(column, whereRight);
        final SqlLimit sqlLimit = new SqlLimit(5, 10);
        final List<SqlNode> orderByExpressions = new ArrayList<>();
        orderByExpressions.add(from);
        final SqlOrderBy orderBy = new SqlOrderBy(orderByExpressions, List.of(false), List.of(true));
        final SqlStatementSelect select = SqlStatementSelect.builder()
                .selectList(SqlSelectList.createAnyValueSelectList()).fromClause(from).whereClause(where)
                .limit(sqlLimit).orderBy(orderBy).build();
        assertThat(sqlGenerationVisitor.visit(select), equalTo(
                "SELECT true FROM \"\"\" t1 '\" WHERE \"\"\" a '\" < '\" right ''' ORDER BY \"\"\" t1 '\" DESC LIMIT 5 OFFSET 10"));
    }

    @Test
    void testVisitSqlJoinQuoting() throws AdapterException {
        final SqlNode left = new SqlTable("\" t1 '", new TableMetadata("t", "", Collections.emptyList(), ""));
        final SqlNode right = new SqlTable("\" t2 '", new TableMetadata("t", "", Collections.emptyList(), ""));
        final SqlNode conditionLeft = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlNode conditionRight = new SqlColumn(1,
                ColumnMetadata.builder().name("\" b '").type(DataType.createBool()).build());
        final SqlNode condition = new SqlPredicateEqual(conditionLeft, conditionRight);
        final SqlJoin sqlJoin = new SqlJoin(left, right, condition, JoinType.INNER);
        assertThat(sqlGenerationVisitor.visit(sqlJoin),
                equalTo("\"\"\" t1 '\" INNER JOIN \"\"\" t2 '\" ON \"\"\" a '\" = \"\"\" b '\""));
    }

    @Test
    void testVisitSqlFunctionAggregateListaggQuoting() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("a \"'").type(DataType.createBool()).build());
        final SqlFunctionAggregateListagg.Behavior overflowBehavior = new SqlFunctionAggregateListagg.Behavior(
                SqlFunctionAggregateListagg.BehaviorType.TRUNCATE);
        overflowBehavior.setTruncationType(WITH_COUNT);
        overflowBehavior.setTruncationFiller(new SqlLiteralString("\" filler '"));
        final List<SqlNode> expressions = new ArrayList<>();
        expressions.add(new SqlColumn(1, ColumnMetadata.builder().name("b \"'").type(DataType.createBool()).build()));
        final SqlOrderBy orderBy = new SqlOrderBy(expressions, List.of(false), List.of(true));
        final SqlFunctionAggregateListagg listagg = SqlFunctionAggregateListagg.builder(argument, overflowBehavior)
                .orderBy(orderBy).separator(new SqlLiteralString("\" separator '")).build();
        assertThat(sqlGenerationVisitor.visit(listagg), equalTo(
                "LISTAGG(\"a \"\"'\", '\" separator ''' ON OVERFLOW TRUNCATE '\" filler ''' WITH COUNT) WITHIN GROUP (ORDER BY \"b \"\"'\" DESC)"));
    }

    @Test
    void testVisitSqlFunctionGroupConcatQuoting() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlOrderBy orderBy = new SqlOrderBy(List.of(argument), List.of(false), List.of(true));
        final SqlFunctionAggregateGroupConcat groupConcat = SqlFunctionAggregateGroupConcat.builder(argument)
                .orderBy(orderBy).separator(new SqlLiteralString("this string \" contains a ' character")).build();
        assertThat(sqlGenerationVisitor.visit(groupConcat), equalTo(
                "GROUP_CONCAT(\"\"\" a '\" ORDER BY \"\"\" a '\" DESC SEPARATOR 'this string \" contains a '' character')"));
    }

    @Test
    void testVisitSqlLiteralDateQuoting() {
        final SqlLiteralDate sqlLiteralDate = new SqlLiteralDate("2015-12-01 \" '");
        assertThat(sqlGenerationVisitor.visit(sqlLiteralDate), equalTo("DATE '2015-12-01 \" '''"));
    }

    @Test
    void testVisitSqlLiteralTimestampQuoting() {
        final SqlLiteralTimestamp sqlLiteralTimestamp = new SqlLiteralTimestamp("2019-02-12 12:07:00 \" '");
        assertThat(sqlGenerationVisitor.visit(sqlLiteralTimestamp), equalTo("TIMESTAMP '2019-02-12 12:07:00 \" '''"));
    }

    @Test
    void testVisitSqlLiteralTimestampUtcQuoting() {
        final SqlLiteralTimestampUtc sqlLiteralTimestampUtc = new SqlLiteralTimestampUtc("2019-02-07 23:59:00 \" '");
        assertThat(sqlGenerationVisitor.visit(sqlLiteralTimestampUtc),
                equalTo("TIMESTAMP '2019-02-07 23:59:00 \" '''"));
    }

    @Test
    void testVisitSqlLiteralStringQuoting() {
        final SqlLiteralString sqlLiteralString = new SqlLiteralString("some string \" ' ");
        assertThat(sqlGenerationVisitor.visit(sqlLiteralString), equalTo("'some string \" '' '"));
    }

    @Test
    void testVisitSqlLiteralIntervalYearToMonthQuoting() {
        final SqlLiteralInterval sqlLiteralString = new SqlLiteralInterval("100-1 \" ' ",
                DataType.createIntervalYearMonth(3));
        assertThat(sqlGenerationVisitor.visit(sqlLiteralString), equalTo("INTERVAL '100-1 \" '' ' YEAR (3) TO MONTH"));
    }

    @Test
    void testVisitSqlLiteralIntervalDayToSecondQuoting() {
        final SqlLiteralInterval sqlLiteralString = new SqlLiteralInterval("2 23:10:59 \" ' ",
                DataType.createIntervalDaySecond(3, 2));
        assertThat(sqlGenerationVisitor.visit(sqlLiteralString),
                equalTo("INTERVAL '2 23:10:59 \" '' ' DAY (3) TO SECOND (2)"));
    }

    @Test
    void testVisitSelectListQuoting() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("a \"'").type(DataType.createBool()).build()));
        final SqlSelectList sqlSelectList = SqlSelectList.createRegularSelectList(arguments);
        assertThat(sqlGenerationVisitor.visit(sqlSelectList), equalTo("\"a \"\"'\""));
    }

    @Test
    void testVisitSqlColumnQuoting() throws AdapterException {
        final SqlColumn sqlColumn = new SqlColumn(1,
                ColumnMetadata.builder().name("a \"'").type(DataType.createBool()).build(), "t \" '");
        assertThat(sqlGenerationVisitor.visit(sqlColumn), equalTo("\"t \"\" '\".\"a \"\"'\""));
    }

    @Test
    void testVisitSqlColumnWithAliasQuoting() throws AdapterException {
        final SqlColumn sqlColumn = new SqlColumn(1,
                ColumnMetadata.builder().name("a \"'").type(DataType.createBool()).build(), "t", "alias \" '");
        assertThat(sqlGenerationVisitor.visit(sqlColumn), equalTo("\"alias \"\" '\".\"a \"\"'\""));
    }

    @Test
    void testVisitSqlTableQuoting() {
        final SqlTable sqlTable = new SqlTable("t \" '", new TableMetadata("t", "", Collections.emptyList(), ""));
        assertThat(sqlGenerationVisitor.visit(sqlTable), equalTo("\"t \"\" '\""));
    }

    @Test
    void testVisitSqlTableCatalogAndSchemaQualifiedQuoting() {
        final SqlDialect sqlDialect = new TestDialect(null, adapterProperties, null);
        final SqlGenerationContext context = new SqlGenerationContext("catalog \" '", "schema \" '", false);
        final SqlGenerationVisitor sqlGenerationVisitor = new SqlGenerationVisitor(sqlDialect, context);
        final SqlTable sqlTable = new SqlTable("t \" '", "alias \" '",
                new TableMetadata("t", "", Collections.emptyList(), ""));
        assertThat(sqlGenerationVisitor.visit(sqlTable),
                equalTo("\"catalog \"\" '\".\"schema \"\" '\".\"t \"\" '\" \"alias \"\" '\""));
    }

    @Test
    void testVisitGroupByQuoting() throws AdapterException {
        final SqlGroupBy groupBy = new SqlGroupBy(List.of( //
                new SqlColumn(1, ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build()), //
                new SqlColumn(2, ColumnMetadata.builder().name("\" b '").type(DataType.createBool()).build())));
        assertThat(sqlGenerationVisitor.visit(groupBy), equalTo("\"\"\" a '\", \"\"\" b '\""));
    }

    @Test
    void testVisitGroupByReplacesIntegerLiterals() throws AdapterException {
        final SqlGroupBy groupBy = new SqlGroupBy(List.of( //
                new SqlColumn(1, ColumnMetadata.builder().name("a").type(DataType.createBool()).build()), //
                new SqlLiteralExactnumeric(BigDecimal.valueOf(42))));
        assertThat(sqlGenerationVisitor.visit(groupBy), equalTo("\"a\", '42'"));
    }

    @Test
    void testVisitGroupByDoesNotModifyStringLiterals() throws AdapterException {
        final SqlGroupBy groupBy = new SqlGroupBy(List.of(new SqlLiteralString("literal")));
        assertThat(sqlGenerationVisitor.visit(groupBy), equalTo("'literal'"));
    }

    @Test
    void testVisitSqlFunctionAggregateQuoting() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build()));
        arguments.add(new SqlColumn(2, ColumnMetadata.builder().name("\" b '").type(DataType.createBool()).build()));
        final SqlFunctionAggregate function = new SqlFunctionAggregate(AggregateFunction.COUNT, arguments, true);
        assertThat(sqlGenerationVisitor.visit(function), equalTo("COUNT(DISTINCT (\"\"\" a '\", \"\"\" b '\"))"));
    }

    @Test
    void testVisitSqlFunctionScalarQuoting() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build()));
        arguments.add(new SqlColumn(2, ColumnMetadata.builder().name("\" b '").type(DataType.createBool()).build()));
        final SqlFunctionScalar function = new SqlFunctionScalar(ScalarFunction.ADD, arguments);
        assertThat(sqlGenerationVisitor.visit(function), equalTo("(\"\"\" a '\" + \"\"\" b '\")"));
    }

    @Test
    void testVisitSqlFunctionScalarCastQuoting() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlFunctionScalarCast function = new SqlFunctionScalarCast(DataType.createBool(), argument);
        assertThat(sqlGenerationVisitor.visit(function), equalTo("CAST(\"\"\" a '\" AS BOOLEAN)"));
    }

    @Test
    void testVisitSqlFunctionScalarCaseQuoting() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlLiteralString("\" 1 '"));
        arguments.add(new SqlLiteralString("\" 10 '"));
        final List<SqlNode> results = new ArrayList<>();
        results.add(new SqlLiteralString("\" one '"));
        results.add(new SqlLiteralString("\" ten '"));
        results.add(new SqlLiteralString("\" more '"));
        final SqlColumn basis = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlFunctionScalarCase function = new SqlFunctionScalarCase(arguments, results, basis);
        assertThat(sqlGenerationVisitor.visit(function), equalTo(
                "CASE \"\"\" a '\" WHEN '\" 1 ''' THEN '\" one ''' WHEN '\" 10 ''' THEN '\" ten ''' ELSE '\" more ''' END"));
    }

    @Test
    void testVisitSqlFunctionScalaExtractQuoting() throws AdapterException {
        final SqlColumn argument = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlFunctionScalarExtract function = new SqlFunctionScalarExtract(YEAR, argument);
        assertThat(sqlGenerationVisitor.visit(function), equalTo("EXTRACT(YEAR FROM \"\"\" a '\")"));
    }

    @Test
    void testVisitSqlFunctionScalarJsonValueQuoting() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlLiteralString("\" {} '"));
        arguments.add(new SqlLiteralString("\" $.a '"));
        final Behavior emptyBehavior = new Behavior(BehaviorType.DEFAULT,
                Optional.of(new SqlLiteralString("\" *** error *** '")));
        final Behavior errorBehavior = new Behavior(BehaviorType.DEFAULT,
                Optional.of(new SqlLiteralString("\" *** error *** '")));
        final SqlFunctionScalarJsonValue sqlFunctionScalarJsonValue = new SqlFunctionScalarJsonValue(
                ScalarFunction.JSON_VALUE, arguments, DataType.createVarChar(1000, DataType.ExaCharset.UTF8),
                emptyBehavior, errorBehavior);
        assertThat(sqlGenerationVisitor.visit(sqlFunctionScalarJsonValue),
                equalTo("JSON_VALUE('\" {} ''', '\" $.a ''' RETURNING VARCHAR(1000) UTF8 "
                        + "DEFAULT '\" *** error *** ''' ON EMPTY DEFAULT '\" *** error *** ''' ON ERROR)"));
    }

    @Test
    void testVisitSqlPredicateAndQuoting() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build()));
        arguments.add(new SqlColumn(2, ColumnMetadata.builder().name("\" b '").type(DataType.createBool()).build()));
        final SqlPredicateAnd sqlPredicateAnd = new SqlPredicateAnd(arguments);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateAnd), equalTo("(\"\"\" a '\" AND \"\"\" b '\")"));
    }

    @Test
    void testVisitSqlPredicateBetweenQuoting() throws AdapterException {
        final SqlNode expression = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlNode betweenLeft = new SqlLiteralString("\" left '");
        final SqlNode betweenRight = new SqlLiteralString("\" right '");
        final SqlPredicateBetween sqlPredicateBetween = new SqlPredicateBetween(expression, betweenLeft, betweenRight);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateBetween),
                equalTo("\"\"\" a '\" BETWEEN '\" left ''' AND '\" right '''"));
    }

    @Test
    void testVisitSqlPredicateEqualQuoting() throws AdapterException {
        final SqlNode left = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlNode right = new SqlLiteralString("\" right '");
        final SqlPredicateEqual sqlPredicateEqual = new SqlPredicateEqual(left, right);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateEqual), equalTo("\"\"\" a '\" = '\" right '''"));
    }

    @Test
    void testVisitSqlPredicateInConstListQuoting() throws AdapterException {
        final SqlNode expression = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlLiteralString("\" one '"));
        arguments.add(new SqlLiteralString("\" two '"));
        final SqlPredicateInConstList predicateInConstList = new SqlPredicateInConstList(expression, arguments);
        assertThat(sqlGenerationVisitor.visit(predicateInConstList),
                equalTo("\"\"\" a '\" IN ('\" one ''', '\" two ''')"));
    }

    @Test
    void testVisitSqlPredicateIsJsonQuoting() throws AdapterException {
        final SqlNode expression = new SqlLiteralString("\" {\"a\": 1} '");
        final SqlPredicateIsJson sqlPredicateIsJson = new SqlPredicateIsJson(expression,
                AbstractSqlPredicateJson.TypeConstraints.OBJECT,
                AbstractSqlPredicateJson.KeyUniquenessConstraint.WITH_UNIQUE_KEYS);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsJson),
                equalTo("'\" {\"a\": 1} ''' IS JSON OBJECT WITH UNIQUE"));
    }

    @Test
    void testVisitSqlPredicateIsNotJsonQuoting() throws AdapterException {
        final SqlNode expression = new SqlLiteralString("\" {\"a\": 1} '");
        final SqlPredicateIsNotJson sqlPredicateIsNotJson = new SqlPredicateIsNotJson(expression,
                AbstractSqlPredicateJson.TypeConstraints.OBJECT,
                AbstractSqlPredicateJson.KeyUniquenessConstraint.WITH_UNIQUE_KEYS);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsNotJson),
                equalTo("'\" {\"a\": 1} ''' IS NOT JSON OBJECT WITH UNIQUE"));
    }

    @Test
    void testVisitSqlPredicateLessQuoting() throws AdapterException {
        final SqlNode left = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlNode right = new SqlLiteralString("\" right '");
        final SqlPredicateLess sqlPredicateLess = new SqlPredicateLess(left, right);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateLess), equalTo("\"\"\" a '\" < '\" right '''"));
    }

    @Test
    void testVisitSqlPredicateLessEqualQuoting() throws AdapterException {
        final SqlNode left = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlNode right = new SqlLiteralString("\" right '");
        final SqlPredicateLessEqual sqlPredicateLessEqual = new SqlPredicateLessEqual(left, right);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateLessEqual), equalTo("\"\"\" a '\" <= '\" right '''"));
    }

    @Test
    void testVisitSqlPredicateLikeQuoting() throws AdapterException {
        final SqlNode left = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlNode pattern = new SqlLiteralString("\" pattern '");
        final SqlNode escapeChar = new SqlLiteralString("\" escape '");
        final SqlPredicateLike sqlPredicateLike = new SqlPredicateLike(left, pattern, escapeChar);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateLike),
                equalTo("\"\"\" a '\" LIKE '\" pattern ''' ESCAPE '\" escape '''"));
    }

    @Test
    void testVisitSqlPredicateLikeRegexpQuoting() throws AdapterException {
        final SqlNode left = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlNode pattern = new SqlLiteralString("\" pattern '");
        final SqlPredicateLikeRegexp sqlPredicateLike = new SqlPredicateLikeRegexp(left, pattern);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateLike), equalTo("\"\"\" a '\" REGEXP_LIKE '\" pattern '''"));
    }

    @Test
    void testVisitSqlPredicateNotQuoting() throws AdapterException {
        final SqlNode expression = new SqlLiteralString("\" value '");
        final SqlPredicateNot sqlPredicateNot = new SqlPredicateNot(expression);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateNot), equalTo("NOT ('\" value ''')"));
    }

    @Test
    void testVisitSqlPredicateNotEqualQuoting() throws AdapterException {
        final SqlNode left = new SqlColumn(1,
                ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build());
        final SqlNode right = new SqlLiteralString("\" right '");
        final SqlPredicateNotEqual sqlPredicateNotEqual = new SqlPredicateNotEqual(left, right);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateNotEqual), equalTo("\"\"\" a '\" <> '\" right '''"));
    }

    @Test
    void testVisitSqlPredicateOrQuoting() throws AdapterException {
        final List<SqlNode> arguments = new ArrayList<>();
        arguments.add(new SqlColumn(1, ColumnMetadata.builder().name("\" a '").type(DataType.createBool()).build()));
        arguments.add(new SqlColumn(2, ColumnMetadata.builder().name("\" b '").type(DataType.createBool()).build()));
        final SqlPredicateOr sqlPredicateOr = new SqlPredicateOr(arguments);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateOr), equalTo("(\"\"\" a '\" OR \"\"\" b '\")"));
    }

    @Test
    void testVisitSqlPredicateIsNullQuoting() throws AdapterException {
        final SqlNode expression = new SqlLiteralString("\" value '");
        final SqlPredicateIsNull sqlPredicateIsNull = new SqlPredicateIsNull(expression);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsNull), equalTo("('\" value ''') IS NULL"));
    }

    @Test
    void testVisitSqlPredicateIsNotNullQuoting() throws AdapterException {
        final SqlNode expression = new SqlLiteralString("\" value '");
        final SqlPredicateIsNotNull sqlPredicateIsNull = new SqlPredicateIsNotNull(expression);
        assertThat(sqlGenerationVisitor.visit(sqlPredicateIsNull), equalTo("('\" value ''') IS NOT NULL"));
    }

    @Test
    void visitBinaryScalarFunctionThrowsException() {
        final SqlFunctionScalar functionScalar = new SqlFunctionScalar(ScalarFunction.ADD, List.of());
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sqlGenerationVisitor.visit(functionScalar));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-11"));
    }

    @Test
    void visitPrefixScalarFunctionThrowsException() {
        final SqlFunctionScalar functionScalar = new SqlFunctionScalar(ScalarFunction.NEG, List.of());
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sqlGenerationVisitor.visit(functionScalar));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-12"));
    }

    private static class TestDialect extends DummySqlDialect {
        public TestDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties,
                    final ExaMetadata exaMetadata) {
            super(connectionFactory, properties, exaMetadata);
        }

        @Override
        public boolean requiresCatalogQualifiedTableNames(final SqlGenerationContext context) {
            return true;
        }

        @Override
        public boolean requiresSchemaQualifiedTableNames(final SqlGenerationContext context) {
            return true;
        }
    }
}