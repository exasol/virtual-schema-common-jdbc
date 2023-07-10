package com.exasol.adapter.dialects.rewriting;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.adapternotes.ColumnAdapterNotesJsonConverter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlGenerator;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.sql.*;
import com.exasol.adapter.sql.SqlFunctionAggregateListagg.Behavior;
import com.exasol.adapter.sql.SqlFunctionAggregateListagg.BehaviorType;
import com.exasol.errorreporting.ExaError;

/**
 * This class has the logic to generate SQL queries based on a graph of {@link SqlNode} elements. It uses the visitor
 * pattern. This class interacts with the dialects in some situations, e.g. to find out how to handle quoting,
 * case-sensitivity.
 *
 * <p>
 * If this class is not sufficiently customizable for your use case, you can extend this class and override the required
 * methods. You also have to return your custom visitor class then in the method
 * {@link SqlDialect#getSqlGenerator(SqlGenerationContext)}.
 * </p>
 *
 * Note on operator associativity and parenthesis generation: Currently we almost always use parenthesis. Without
 * parenthesis, two {@link SqlNode} graphs with different semantic lead to {@code select 1 = 1 - 1 + 1}. Also
 * {@code SELECT NOT NOT TRUE} needs to be written as {@code SELECT NOT (NOT TRUE)} to work at all, whereas
 * {@code SELECT NOT TRUE} works fine without parentheses. Currently we make inflationary use of parenthesis to enforce
 * the right semantic, but hopefully there is a better way.
 */
public class SqlGenerationVisitor implements SqlNodeVisitor<String>, SqlGenerator {
    private final SqlDialect dialect;
    private final SqlGenerationContext context;

    /**
     * Creates a new instance of the {@link SqlGenerationVisitor}.
     *
     * @param dialect SQl dialect
     * @param context SQL generation context
     */
    public SqlGenerationVisitor(final SqlDialect dialect, final SqlGenerationContext context) {
        this.dialect = dialect;
        this.context = context;
        checkDialectAliases();
    }

    @Override
    public String generateSqlFor(final SqlNode sqlNode) throws AdapterException {
        return sqlNode.accept(this);
    }

    /**
     * Get the SQL dialect.
     *
     * @return SQL dialect.
     */
    protected SqlDialect getDialect() {
        return this.dialect;
    }

    /**
     * Check if dialect provided invalid aliases, which would never be applied.
     */
    protected void checkDialectAliases() {
        for (final ScalarFunction function : this.dialect.getScalarFunctionAliases().keySet()) {
            if (!function.isSimple()) {
                throw new UnsupportedOperationException(ExaError.messageBuilder("E-VSCJDBC-9")
                        .message(
                                "The dialect {{dialectName|uq}} provided an alias for the non-simple scalar function "
                                        + "{{functionName|uq}}. This alias will never be considered.",
                                this.dialect.getName(), function.name())
                        .ticketMitigation().toString());
            }
        }
        for (final AggregateFunction function : this.dialect.getAggregateFunctionAliases().keySet()) {
            if (!function.isSimple()) {
                throw new UnsupportedOperationException(ExaError.messageBuilder("E-VSCJDBC-10").message(
                        "The dialect {{dialectName|uq}} provided an alias for the non-simple aggregate function "
                                + "{{functionName|uq}}. This alias will never be considered.",
                        this.dialect.getName(), function.name()).ticketMitigation().toString());
            }
        }
    }

    /**
     * Check if a column is directly in the select list.
     *
     * @param column column name
     * @return {@code true} if the column is directly in the select list of a query
     */
    protected boolean isDirectlyInSelectList(final SqlColumn column) {
        return column.hasParent() && (column.getParent().getType() == SqlNodeType.SELECT_LIST);
    }

    @Override
    public String visit(final SqlStatementSelect select) throws AdapterException {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(select.getSelectList().accept(this));
        sql.append(" FROM ");
        sql.append(select.getFromClause().accept(this));
        if (select.hasFilter()) {
            sql.append(" WHERE ");
            sql.append(select.getWhereClause().accept(this));
        }
        if (select.hasGroupBy()) {
            sql.append(" GROUP BY ");
            sql.append(select.getGroupBy().accept(this));
        }
        if (select.hasHaving()) {
            sql.append(" HAVING ");
            sql.append(select.getHaving().accept(this));
        }
        if (select.hasOrderBy()) {
            sql.append(" ");
            sql.append(select.getOrderBy().accept(this));
        }
        if (select.hasLimit()) {
            sql.append(" ");
            sql.append(select.getLimit().accept(this));
        }
        return sql.toString();
    }

    @Override
    public String visit(final SqlSelectList selectList) throws AdapterException {
        if (selectList.hasExplicitColumnsList()) {
            return createExplicitColumnsSelectList(selectList);
        } else {
            return representAnyColumnInSelectList();
        }
    }

    /**
     * Represent "any column" in the <code>SELECT</code> list.
     * <p>
     * A typical example are queries where the only thing that matters if what you searched for exists or not. Different
     * databases have different preferred ways of expressing this kind of result.
     * </p>
     *
     * @return always <code>"true"</code>
     */
    protected String representAnyColumnInSelectList() {
        return SqlConstants.TRUE;
    }

    /**
     * Create a list of explicitly specified columns (where columns can also be expressions) for a <code>SELECT</code>
     * list.
     *
     * @param selectList list of columns (or expressions) in the <code>SELECT</code> part
     * @return string representing the <code>SELECT</code> list
     * @throws AdapterException in case the expressions in the list cannot be rendered to SQL
     */
    protected String createExplicitColumnsSelectList(final SqlSelectList selectList) throws AdapterException {
        final List<SqlNode> expressions = selectList.getExpressions();
        final List<String> selectElement = new ArrayList<>(expressions.size());
        for (final SqlNode node : expressions) {
            selectElement.add(node.accept(this));
        }
        return String.join(", ", selectElement);
    }

    @Override
    public String visit(final SqlColumn column) throws AdapterException {
        String tablePrefix = "";
        if (column.hasTableAlias()) {
            tablePrefix = this.dialect.applyQuote(column.getTableAlias())
                    + this.dialect.getTableCatalogAndSchemaSeparator();
        } else if ((column.getTableName() != null) && !column.getTableName().isEmpty()) {
            tablePrefix = this.dialect.applyQuote(column.getTableName())
                    + this.dialect.getTableCatalogAndSchemaSeparator();
        }
        return tablePrefix + this.dialect.applyQuote(column.getName());
    }

    @Override
    public String visit(final SqlTable table) {
        String schemaPrefix = "";
        if (this.dialect.requiresCatalogQualifiedTableNames(this.context) && (this.context.getCatalogName() != null)
                && !this.context.getCatalogName().isEmpty()) {
            schemaPrefix = this.dialect.applyQuote(this.context.getCatalogName())
                    + this.dialect.getTableCatalogAndSchemaSeparator();
        }
        if (this.dialect.requiresSchemaQualifiedTableNames(this.context) && (this.context.getSchemaName() != null)
                && !this.context.getSchemaName().isEmpty()) {
            schemaPrefix += this.dialect.applyQuote(this.context.getSchemaName())
                    + this.dialect.getTableCatalogAndSchemaSeparator();
        }
        if (table.hasAlias()) {
            return schemaPrefix + this.dialect.applyQuote(table.getName()) + " "
                    + this.dialect.applyQuote(table.getAlias());
        } else {
            return schemaPrefix + this.dialect.applyQuote(table.getName());
        }
    }

    @Override
    public String visit(final SqlJoin join) throws AdapterException {
        return join.getLeft().accept(this) + " " + join.getJoinType().name().replace('_', ' ') + " JOIN "
                + join.getRight().accept(this) + " ON " + join.getCondition().accept(this);
    }

    @Override
    public String visit(final SqlFunctionAggregateListagg sqlFunctionAggregateListagg) throws AdapterException {
        final StringBuilder builder = new StringBuilder();
        builder.append("LISTAGG(");
        if (sqlFunctionAggregateListagg.hasDistinct()) {
            builder.append("DISTINCT ");
        }
        builder.append(sqlFunctionAggregateListagg.getArgument().accept(this));
        if (sqlFunctionAggregateListagg.hasSeparator()) {
            builder.append(", ");
            builder.append(sqlFunctionAggregateListagg.getSeparator().accept(this));
        }
        builder.append(" ON OVERFLOW ");
        final Behavior overflowBehavior = sqlFunctionAggregateListagg.getOverflowBehavior();
        builder.append(overflowBehavior.getBehaviorType());
        if (overflowBehavior.getBehaviorType() == BehaviorType.TRUNCATE) {
            if (overflowBehavior.hasTruncationFiller()) {
                builder.append(" ");
                builder.append(overflowBehavior.getTruncationFiller().accept(this));
            }
            builder.append(" ");
            builder.append(overflowBehavior.getTruncationType());
        }
        builder.append(")");
        if (sqlFunctionAggregateListagg.hasOrderBy()) {
            builder.append(" WITHIN GROUP (");
            builder.append(sqlFunctionAggregateListagg.getOrderBy().accept(this));
            builder.append(")");
        }
        return builder.toString();
    }

    @Override
    public String visit(final SqlGroupBy groupBy) throws AdapterException {
        final List<String> selectElement = new ArrayList<>();
        for (final SqlNode node : groupBy.getExpressions()) {
            selectElement.add(node.accept(this));
        }
        return String.join(", ", selectElement);
    }

    @Override
    public String visit(final SqlFunctionAggregate function) throws AdapterException {
        final StringBuilder builder = new StringBuilder();
        final String functionNameInSourceSystem = this.dialect.getAggregateFunctionAliases()
                .getOrDefault(function.getFunction(), function.getFunctionName());
        builder.append(functionNameInSourceSystem);
        builder.append("(");
        final List<String> renderedArguments = new ArrayList<>();
        for (final SqlNode node : function.getArguments()) {
            renderedArguments.add(node.accept(this));
        }
        final boolean countFunction = function.getFunctionName().equals("COUNT");
        if (function.hasDistinct()) {
            builder.append("DISTINCT ");
        }
        if (countFunction && (renderedArguments.size() > 1)) {
            builder.append("(");
        } else if (countFunction && renderedArguments.isEmpty()) {
            renderedArguments.add(SqlConstants.ASTERISK);
        }
        builder.append(String.join(", ", renderedArguments));
        if (countFunction && (renderedArguments.size() > 1)) {
            builder.append(")");
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visit(final SqlFunctionAggregateGroupConcat function) throws AdapterException {
        final StringBuilder builder = new StringBuilder();
        builder.append(function.getFunctionName());
        builder.append("(");
        if (function.hasDistinct()) {
            builder.append("DISTINCT ");
        }
        builder.append(function.getArgument().accept(this));
        if (function.hasOrderBy()) {
            builder.append(" ");
            final String orderByString = function.getOrderBy().accept(this);
            builder.append(orderByString);
        }
        if (function.getSeparator() != null) {
            builder.append(" SEPARATOR ");
            builder.append(function.getSeparator().accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visit(final SqlFunctionScalar function) throws AdapterException {
        final List<String> argumentsSql = this.generateSqlForFunctionArguments(function.getArguments());
        if (this.hasAlias(function)) {
            return generateSqlForFunctionWithAlias(function, argumentsSql);
        } else if (this.isBinaryInfixFunction(function)) {
            return this.generateSqlForBinaryInfixFunction(function.getFunction(), argumentsSql);
        } else if (this.isPrefixFunction(function)) {
            return this.generateSqlForPrefixFunction(function.getFunction(), argumentsSql);
        } else {
            return this.generateSqlForFunctionWithName(function, argumentsSql, function.getFunctionName());
        }
    }

    private List<String> generateSqlForFunctionArguments(final List<SqlNode> arguments) throws AdapterException {
        final List<String> argumentsSql = new ArrayList<>();
        for (final SqlNode argument : arguments) {
            argumentsSql.add(this.generateSqlFor(argument));
        }
        return argumentsSql;
    }

    private boolean hasAlias(final SqlFunctionScalar function) {
        return this.dialect.getScalarFunctionAliases().containsKey(function.getFunction());
    }

    private String generateSqlForFunctionWithAlias(final SqlFunctionScalar function, final List<String> sqlArguments) {
        final String alias = this.dialect.getScalarFunctionAliases().get(function.getFunction());
        return this.generateSqlForFunctionWithName(function, sqlArguments, alias);
    }

    private String generateSqlForFunctionWithName(final SqlFunctionScalar function, final List<String> sqlArguments,
            final String functionName) {
        if (sqlArguments.isEmpty() && this.dialect.omitParentheses(function.getFunction())) {
            return functionName;
        } else {
            return functionName + "(" + String.join(", ", sqlArguments) + ")";
        }
    }

    private boolean isBinaryInfixFunction(final SqlFunctionScalar function) {
        return this.dialect.getBinaryInfixFunctionAliases().containsKey(function.getFunction());
    }

    private String generateSqlForBinaryInfixFunction(final ScalarFunction scalarFunction,
            final List<String> sqlArguments) {
        final String realFunctionName = this.dialect.getBinaryInfixFunctionAliases().get(scalarFunction);
        if (sqlArguments.size() != 2) {
            throw new IllegalArgumentException(ExaError.messageBuilder("E-VSCJDBC-11").message(
                    "The {{realFunctionName|uq}} function requests 2 arguments, but {{sqlArgumentsSize|uq}} were given.",
                    realFunctionName, sqlArguments.size()).toString());
        }
        return "(" + sqlArguments.get(0) + " " + realFunctionName + " " + sqlArguments.get(1) + ")";
    }

    private boolean isPrefixFunction(final SqlFunctionScalar function) {
        return this.dialect.getPrefixFunctionAliases().containsKey(function.getFunction());
    }

    private String generateSqlForPrefixFunction(final ScalarFunction scalarFunction, final List<String> sqlArguments) {
        final String realFunctionName = this.dialect.getPrefixFunctionAliases().get(scalarFunction);
        if (sqlArguments.size() != 1) {
            throw new IllegalArgumentException(ExaError.messageBuilder("E-VSCJDBC-12").message(
                    "The {{realFunctionName|uq}} function requests 1 argument, but {{sqlArgumentsSize|uq}} were given.",
                    realFunctionName, sqlArguments.size()).toString());
        }
        return "(" + realFunctionName + sqlArguments.get(0) + ")";
    }

    @Override
    public String visit(final SqlFunctionScalarCase function) throws AdapterException {
        final StringBuilder builder = new StringBuilder();
        builder.append("CASE");
        if (function.getBasis() != null) {
            builder.append(" ");
            builder.append(function.getBasis().accept(this));
        }
        for (int i = 0; i < function.getArguments().size(); i++) {
            final SqlNode node = function.getArguments().get(i);
            final SqlNode result = function.getResults().get(i);
            builder.append(" WHEN ");
            builder.append(node.accept(this));
            builder.append(" THEN ");
            builder.append(result.accept(this));
        }
        if (function.getResults().size() > function.getArguments().size()) {
            builder.append(" ELSE ");
            builder.append(function.getResults().get(function.getResults().size() - 1).accept(this));
        }
        builder.append(" END");
        return builder.toString();
    }

    @Override
    public String visit(final SqlFunctionScalarCast function) throws AdapterException {
        final String expression = function.getArgument().accept(this);
        return "CAST(" + expression + " AS " + function.getDataType() + ")";
    }

    @Override
    public String visit(final SqlFunctionScalarExtract function) throws AdapterException {
        final String expression = function.getArgument().accept(this);
        return "EXTRACT(" + function.getToExtract() + " FROM " + expression + ")";
    }

    @Override
    public String visit(final SqlFunctionScalarJsonValue function) throws AdapterException {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("JSON_VALUE(");
        stringBuilder.append(function.getArguments().get(0).accept(this));
        stringBuilder.append(", ");
        stringBuilder.append(function.getArguments().get(1).accept(this));
        stringBuilder.append(" RETURNING ");
        stringBuilder.append(function.getReturningDataType().toString());
        stringBuilder.append(" ");
        final SqlFunctionScalarJsonValue.Behavior emptyBehavior = function.getEmptyBehavior();
        stringBuilder.append(emptyBehavior.getBehaviorType());
        final Optional<SqlNode> emptyBehaviorExpression = emptyBehavior.getExpression();
        if (emptyBehaviorExpression.isPresent()) {
            stringBuilder.append(" ");
            stringBuilder.append(emptyBehaviorExpression.get().accept(this));
        }
        stringBuilder.append(" ON EMPTY ");
        final SqlFunctionScalarJsonValue.Behavior errorBehavior = function.getErrorBehavior();
        stringBuilder.append(errorBehavior.getBehaviorType());
        final Optional<SqlNode> errorBehaviorExpression = errorBehavior.getExpression();
        if (errorBehaviorExpression.isPresent()) {
            stringBuilder.append(" ");
            stringBuilder.append(errorBehaviorExpression.get().accept(this));
        }
        stringBuilder.append(" ON ERROR)");
        return stringBuilder.toString();
    }

    @Override
    public String visit(final SqlLimit limit) {
        String query = "LIMIT " + limit.getLimit();
        if (limit.getOffset() != 0) {
            query += " OFFSET " + limit.getOffset();
        }
        return query;
    }

    @Override
    public String visit(final SqlLiteralBool literal) {
        if (literal.getValue()) {
            return SqlConstants.TRUE;
        } else {
            return SqlConstants.FALSE;
        }
    }

    @Override
    public String visit(final SqlLiteralDate literal) {
        return "DATE " + getDialect().getStringLiteral(literal.getValue());
    }

    /**
     * Get a value of the double converted to an E-notation format.
     * <p>
     * For example: 1.234 becomes 1.234E0
     * </p>
     */
    @Override
    public String visit(final SqlLiteralDouble literal) {
        final NumberFormat numFormat = NumberFormat.getNumberInstance(Locale.US);
        if (numFormat instanceof DecimalFormat) {
            ((DecimalFormat) numFormat).applyPattern("0.################E0");
        }
        return numFormat.format(literal.getValue());
    }

    /**
     * Formats a value of the exactnumeric as a plain string without E notation.
     * <p>
     * For example: 1E-35 becomes 0.00000000000000000000000000000000001
     * </p>
     */
    @Override
    public String visit(final SqlLiteralExactnumeric literal) {
        return literal.getValue().toPlainString();
    }

    @Override
    public String visit(final SqlLiteralNull literal) {
        return "NULL";
    }

    @Override
    public String visit(final SqlLiteralString literal) {
        return this.dialect.getStringLiteral(literal.getValue());
    }

    @Override
    public String visit(final SqlLiteralTimestamp literal) {
        return "TIMESTAMP " + getDialect().getStringLiteral(literal.getValue());
    }

    @Override
    public String visit(final SqlLiteralTimestampUtc literal) {
        return "TIMESTAMP " + getDialect().getStringLiteral(literal.getValue());
    }

    @Override
    public String visit(final SqlLiteralInterval literal) {
        if (literal.getDataType().getIntervalType() == DataType.IntervalType.YEAR_TO_MONTH) {
            return "INTERVAL " + getDialect().getStringLiteral(literal.getValue()) + " YEAR ("
                    + literal.getDataType().getPrecision() + ") TO MONTH";
        } else {
            return "INTERVAL " + getDialect().getStringLiteral(literal.getValue()) + " DAY ("
                    + literal.getDataType().getPrecision() + ") TO SECOND ("
                    + literal.getDataType().getIntervalFraction() + ")";
        }
    }

    @Override
    public String visit(final SqlOrderBy orderBy) throws AdapterException {
        // ORDER BY <expr> [ASC/DESC] [NULLS FIRST/LAST]
        // ASC and NULLS LAST are default in EXASOL
        final List<String> sqlOrderElement = new ArrayList<>();
        for (int i = 0; i < orderBy.getExpressions().size(); ++i) {
            String elementSql = orderBy.getExpressions().get(i).accept(this);
            final boolean shallNullsBeAtTheEnd = orderBy.nullsLast().get(i);
            final boolean isAscending = orderBy.isAscending().get(i);
            if (!isAscending) {
                elementSql += " DESC";
            }
            if (shallNullsBeAtTheEnd != nullsAreAtEndByDefault(isAscending, this.dialect.getDefaultNullSorting())) {
                // we have to specify null positioning explicitly, otherwise it would be wrong
                elementSql += (shallNullsBeAtTheEnd) ? " NULLS LAST" : " NULLS FIRST";
            }
            sqlOrderElement.add(elementSql);
        }
        return "ORDER BY " + String.join(", ", sqlOrderElement);
    }

    /**
     * @param isAscending        true if the desired sort order is ascending, false if descending
     * @param defaultNullSorting default null sorting of dialect
     * @return true, if the data source would position nulls at end of the resultset if NULLS FIRST/LAST is not
     *         specified explicitly.
     */
    private boolean nullsAreAtEndByDefault(final boolean isAscending, final SqlDialect.NullSorting defaultNullSorting) {
        if (defaultNullSorting == SqlDialect.NullSorting.NULLS_SORTED_AT_END) {
            return true;
        } else if (defaultNullSorting == SqlDialect.NullSorting.NULLS_SORTED_AT_START) {
            return false;
        } else {
            if (isAscending) {
                return (defaultNullSorting == SqlDialect.NullSorting.NULLS_SORTED_HIGH);
            } else {
                return (defaultNullSorting != SqlDialect.NullSorting.NULLS_SORTED_HIGH);
            }
        }
    }

    @Override
    public String visit(final SqlPredicateAnd predicate) throws AdapterException {
        final List<String> operandsSql = new ArrayList<>();
        for (final SqlNode node : predicate.getAndedPredicates()) {
            operandsSql.add(node.accept(this));
        }
        return "(" + String.join(" AND ", operandsSql) + ")";
    }

    @Override
    public String visit(final SqlPredicateBetween predicate) throws AdapterException {
        return predicate.getExpression().accept(this) + " BETWEEN " + predicate.getBetweenLeft().accept(this) + " AND "
                + predicate.getBetweenRight().accept(this);
    }

    @Override
    public String visit(final SqlPredicateEqual predicate) throws AdapterException {
        return predicate.getLeft().accept(this) + " = " + predicate.getRight().accept(this);
    }

    @Override
    public String visit(final SqlPredicateInConstList predicate) throws AdapterException {
        final List<String> argumentsSql = new ArrayList<>();
        for (final SqlNode node : predicate.getInArguments()) {
            argumentsSql.add(node.accept(this));
        }
        return predicate.getExpression().accept(this) + " IN (" + String.join(", ", argumentsSql) + ")";
    }

    @Override
    public String visit(final SqlPredicateIsJson function) throws AdapterException {
        return visitSqlPredicateJson(function.getExpression(), "IS JSON", function.getTypeConstraint(),
                function.getKeyUniquenessConstraint());
    }

    // We remove KEYS keyword from the query, because Exasol database cannot parse it correctly in some cases.
    // According to the SQL standard, KEYS is optional.
    private String visitSqlPredicateJson(final SqlNode expression, final String functionName,
            final String typeConstraint, final String keyUniquenessConstraint) throws AdapterException {
        final StringBuilder stringBuilder = new StringBuilder();
        final String expressionString = expression.accept(this);
        stringBuilder.append(expressionString);
        stringBuilder.append(" ");
        stringBuilder.append(functionName);
        stringBuilder.append(" ");
        stringBuilder.append(typeConstraint);
        stringBuilder.append(" ");
        stringBuilder.append(keyUniquenessConstraint.replace(" KEYS", ""));
        return stringBuilder.toString();
    }

    @Override
    public String visit(final SqlPredicateIsNotJson function) throws AdapterException {
        return visitSqlPredicateJson(function.getExpression(), "IS NOT JSON", function.getTypeConstraint(),
                function.getKeyUniquenessConstraint());
    }

    @Override
    public String visit(final SqlPredicateLess predicate) throws AdapterException {
        return predicate.getLeft().accept(this) + " < " + predicate.getRight().accept(this);
    }

    @Override
    public String visit(final SqlPredicateLessEqual predicate) throws AdapterException {
        return predicate.getLeft().accept(this) + " <= " + predicate.getRight().accept(this);
    }

    @Override
    public String visit(final SqlPredicateLike predicate) throws AdapterException {
        String sql = predicate.getLeft().accept(this) + " LIKE " + predicate.getPattern().accept(this);
        if (predicate.getEscapeChar() != null) {
            sql += " ESCAPE " + predicate.getEscapeChar().accept(this);
        }
        return sql;
    }

    @Override
    public String visit(final SqlPredicateLikeRegexp predicate) throws AdapterException {
        return predicate.getLeft().accept(this) + " REGEXP_LIKE " + predicate.getPattern().accept(this);
    }

    @Override
    public String visit(final SqlPredicateNot predicate) throws AdapterException {
        // "SELECT NOT NOT TRUE" is invalid syntax, "SELECT NOT (NOT TRUE)" works.
        return "NOT (" + predicate.getExpression().accept(this) + ")";
    }

    @Override
    public String visit(final SqlPredicateNotEqual predicate) throws AdapterException {
        return predicate.getLeft().accept(this) + " <> " + predicate.getRight().accept(this);
    }

    @Override
    public String visit(final SqlPredicateOr predicate) throws AdapterException {
        final List<String> operandsSql = new ArrayList<>();
        for (final SqlNode node : predicate.getOrPredicates()) {
            operandsSql.add(node.accept(this));
        }
        return "(" + String.join(" OR ", operandsSql) + ")";
    }

    @Override
    public String visit(final SqlPredicateIsNull predicate) throws AdapterException {
        return "(" + predicate.getExpression().accept(this) + ") IS NULL";
    }

    @Override
    public String visit(final SqlPredicateIsNotNull predicate) throws AdapterException {
        return "(" + predicate.getExpression().accept(this) + ") IS NOT NULL";
    }

    /**
     * Get the type name for a column
     *
     * @param column column
     * @return type name
     * @throws AdapterException if something goes wrong
     */
    protected String getTypeNameFromColumn(final SqlColumn column) throws AdapterException {
        final ColumnAdapterNotesJsonConverter converter = ColumnAdapterNotesJsonConverter.getInstance();
        return converter.convertFromJsonToColumnAdapterNotes(column.getMetadata().getAdapterNotes(), column.getName())
                .getTypeName();
    }
}