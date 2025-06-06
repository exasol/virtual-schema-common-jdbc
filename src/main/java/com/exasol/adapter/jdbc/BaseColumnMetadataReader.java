package com.exasol.adapter.jdbc;

import static com.exasol.adapter.jdbc.RemoteMetadataReaderConstants.*;
import static com.exasol.adapter.metadata.DataType.ExaCharset.UTF8;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.adapternotes.ColumnAdapterNotes;
import com.exasol.adapter.adapternotes.ColumnAdapterNotesJsonConverter;
import com.exasol.adapter.dialects.IdentifierConverter;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.errorreporting.ExaError;

/**
 * This class implements a mapper that reads column metadata from the remote database and converts it into JDBC
 * information.
 */
public class BaseColumnMetadataReader extends AbstractMetadataReader implements ColumnMetadataReader {
    /** Logger */
    public static final Logger LOGGER = Logger.getLogger(BaseColumnMetadataReader.class.getName());
    /** Key for column name */
    public static final String NAME_COLUMN = "COLUMN_NAME";
    /** Key for data type */
    public static final String DATA_TYPE_COLUMN = "DATA_TYPE";
    /** Key for column size */
    public static final String SIZE_COLUMN = "COLUMN_SIZE";
    /** Key for decimal digits */
    public static final String SCALE_COLUMN = "DECIMAL_DIGITS";
    /** Key for char octet length */
    public static final String CHAR_OCTET_LENGTH_COLUMN = "CHAR_OCTET_LENGTH";
    /** Key for type name */
    public static final String TYPE_NAME_COLUMN = "TYPE_NAME";
    /** Key for the comment */
    public static final String REMARKS_COLUMN = "REMARKS";
    /** Key for the default value */
    public static final String DEFAULT_VALUE_COLUMN = "COLUMN_DEF";
    /** Key for autoincrement */
    public static final String AUTOINCREMENT_COLUMN = "IS_AUTOINCREMENT";
    /** Key for is nullable */
    public static final String NULLABLE_COLUMN = "IS_NULLABLE";
    private static final boolean DEFAULT_NULLABLE = true;

    private static final Pattern NUMBER_TYPE_PATTERN = Pattern.compile("\\s*(\\d+)\\s*,\\s*(\\d+)\\s*");

    private final IdentifierConverter identifierConverter;
    private final boolean supportsTimestampsWithNanoPrecision;

    /**
     * Create a new instance of a {@link ColumnMetadataReader}.
     *
     * @param connection          JDBC connection through which the column metadata is read from the remote database
     * @param properties          user-defined adapter properties
     * @param exaMetadata         metadata of the Exasol database
     * @param identifierConverter converter between source and Exasol identifiers
     */
    public BaseColumnMetadataReader(final Connection connection, final AdapterProperties properties,
                final ExaMetadata exaMetadata, final IdentifierConverter identifierConverter) {
        super(connection, properties, exaMetadata);
        this.identifierConverter = identifierConverter;
        this.supportsTimestampsWithNanoPrecision = ExasolVersion.parse(exaMetadata).atLeast(8, 32);
    }

    /**
     * Whether the Exasol database supports timestamp with precision up to nanoseconds.
     * @return true if Exasol database supports timestamps with nanoseconds precision false otherwise.
     */
    protected boolean supportsTimestampsWithNanoPrecision() {
        return supportsTimestampsWithNanoPrecision;
    }

    /**
     * Map a metadata for a list of columns to Exasol metadata.
     *
     * @param tableName the table for which the columns are mapped
     * @return list of Exasol column metadata objects
     */
    @Override
    public List<ColumnMetadata> mapColumns(final String tableName) {
        return mapColumns(getCatalogNameFilter(), getSchemaNameFilter(), tableName);
    }

    /**
     * Read the columns metadata from a result set.
     *
     * @param catalogName catalog name
     * @param schemaName  schema name
     * @param tableName   table name
     * @return list with column metadata
     */
    protected List<ColumnMetadata> mapColumns(final String catalogName, final String schemaName,
            final String tableName) {
        try (final ResultSet remoteColumns = getColumnMetadata(catalogName, schemaName, tableName)) {
            return getColumnsFromResultSet(remoteColumns);
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(ExaError.messageBuilder("E-VSCJDBC-1").message(
                    "Unable to read column metadata from remote for catalog \"{{catalogName|uq}}\" and schema \"{{schemaName|uq}}\"",
                    schemaName, catalogName).toString(), exception);
        }
    }

    /**
     * Read column metadata from JDBC driver escaping potential SQL wild cards in the names of schema and table.
     *
     * @param catalogName catalog name
     * @param schemaName  schema name, potential SQL wildcards will be escaped
     * @param tableName   table name, potential SQL wildcards will be escaped
     * @return list with metadata for all columns of the respective catalog, schema, and table
     * @throws SQLException in case of failures
     */
    protected ResultSet getColumnMetadata(final String catalogName, final String schemaName, final String tableName)
            throws SQLException {
        final DatabaseMetaData metadata = this.connection.getMetaData();
        final WildcardEscaper wildcards = WildcardEscaper.instance(metadata.getSearchStringEscape());
        return metadata.getColumns(catalogName, //
                schemaName == null ? null : wildcards.escape(schemaName), //
                tableName == null ? null : wildcards.escape(tableName), //
                ANY_COLUMN);
    }

    /**
     * Read column metadata from JDBC driver without escaping potential SQL wild cards in the names of schema and table.
     *
     * @param catalogName       catalog name
     * @param schemaNamePattern schema name pattern, may contain SQL wildcards
     * @param tableNamePattern  table name pattern, may contain SQL wildcards
     * @return list with metadata for all columns of the respective catalog, matching schema name pattern, and table
     *         name pattern
     * @throws SQLException in case of failures
     */
    protected ResultSet getColumnMetadataAllowingPatterns(final String catalogName, final String schemaNamePattern,
            final String tableNamePattern) throws SQLException {
        return this.connection.getMetaData().getColumns(catalogName, schemaNamePattern, tableNamePattern, ANY_COLUMN);
    }

    /**
     * Read the columns result set.
     *
     * @param remoteColumns column result set.
     * @return list of column metadata
     * @throws SQLException if read fails
     */
    protected List<ColumnMetadata> getColumnsFromResultSet(final ResultSet remoteColumns) throws SQLException {
        final List<ColumnMetadata> columns = new ArrayList<>();
        while (remoteColumns.next()) {
            mapOrSkipColumn(remoteColumns, columns);
        }
        return columns;
    }

    /**
     * Read the column metadata from result set if supported. Otherwise, skip.
     *
     * @param remoteColumns column result set
     * @param columns       list to append column to
     * @throws SQLException if read fails
     */
    public void mapOrSkipColumn(final ResultSet remoteColumns, final List<ColumnMetadata> columns) throws SQLException {
        final ColumnMetadata metadata = mapColumn(remoteColumns);
        if (metadata.getType().isSupported()) {
            columns.add(metadata);
        } else {
            LOGGER.fine(() -> "Column \"" + metadata.getName() + "\" of type \"" + metadata.getOriginalTypeName()
                    + "\" not supported by Virtual Schema. Skipping column in mapping.");
        }
    }

    private ColumnMetadata mapColumn(final ResultSet remoteColumn) throws SQLException {
        final JDBCTypeDescription jdbcTypeDescription = readJdbcTypeDescription(remoteColumn);
        final String columnName = readColumnName(remoteColumn);
        final String originalTypeName = readColumnTypeName(remoteColumn);
        final ColumnAdapterNotes columnAdapterNotes = ColumnAdapterNotes.builder() //
                .jdbcDataType(jdbcTypeDescription.getJdbcType()) //
                .typeName(jdbcTypeDescription.getTypeName()) //
                .build();
        final String columnAdapterNotesJson = ColumnAdapterNotesJsonConverter.getInstance()
                .convertToJson(columnAdapterNotes);
        final DataType exasolType = mapJdbcType(jdbcTypeDescription);
        return ColumnMetadata.builder() //
                .name(columnName) //
                .adapterNotes(columnAdapterNotesJson) //
                .type(exasolType) //
                .nullable(isRemoteColumnNullable(remoteColumn, columnName)) //
                .identity(isAutoIncrementColumn(remoteColumn, columnName)) //
                .defaultValue(readDefaultValue(remoteColumn)) //
                .comment(readComment(remoteColumn)) //
                .originalTypeName(originalTypeName) //
                .build();
    }

    /**
     * Read the JDBC type description of a column.
     *
     * @param remoteColumn result set column
     * @return JDBC type description
     * @throws SQLException if read fails
     */
    public JDBCTypeDescription readJdbcTypeDescription(final ResultSet remoteColumn) throws SQLException {
        final int jdbcType = readJdbcDataType(remoteColumn);
        final int decimalScale = readScale(remoteColumn);
        final int precisionOrSize = readPrecisionOrSize(remoteColumn);
        final int charOctetLength = readOctetLength(remoteColumn);
        final String typeName = readTypeName(remoteColumn);
        return new JDBCTypeDescription(jdbcType, decimalScale, precisionOrSize, charOctetLength, typeName);
    }

    private int readJdbcDataType(final ResultSet remoteColumn) throws SQLException {
        return remoteColumn.getInt(DATA_TYPE_COLUMN);
    }

    private int readScale(final ResultSet remoteColumn) throws SQLException {
        return remoteColumn.getInt(SCALE_COLUMN);
    }

    private int readPrecisionOrSize(final ResultSet remoteColumn) throws SQLException {
        return remoteColumn.getInt(SIZE_COLUMN);
    }

    private int readOctetLength(final ResultSet remoteColumn) throws SQLException {
        return remoteColumn.getInt(CHAR_OCTET_LENGTH_COLUMN);
    }

    private String readTypeName(final ResultSet remoteColumn) throws SQLException {
        return remoteColumn.getString(TYPE_NAME_COLUMN);
    }

    /**
     * Check if a column a nullable.
     *
     * @param remoteColumn column result set
     * @param columnName   column name
     * @return {@code true} if remote column is nullable
     */
    protected boolean isRemoteColumnNullable(final ResultSet remoteColumn, final String columnName) {
        try {
            return !JDBC_FALSE.equalsIgnoreCase(remoteColumn.getString(NULLABLE_COLUMN));
        } catch (final SQLException exception) {
            LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-20").message(
                    "Caught an SQL exception trying to determine whether column \"{{columnName|uq}}\" is nullable: "
                            + "{{exceptionMessage|uq}}",
                    columnName, exception.getMessage()).toString());
            LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-38")
                    .message("Assuming column \"{{columnName|uq}}\" to be nullable.", columnName).toString());
            return DEFAULT_NULLABLE;
        }
    }

    private boolean isAutoIncrementColumn(final ResultSet remoteColumn, final String columnName) {
        try {
            final String identity = remoteColumn.getString(AUTOINCREMENT_COLUMN);
            return JDBC_TRUE.equalsIgnoreCase(identity);
        } catch (final SQLException exception) {
            LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-37")
                    .message(
                            "Caught an SQL exception trying to determine whether column \"{{columnName|uq}}\" is "
                                    + "an auto-increment column: {{exceptionMessage|uq}}",
                            columnName, exception.getMessage())
                    .toString());
            LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-36")
                    .message("Assuming  that column \"{{columnName|uq}}\" is not incremented automatically.",
                            columnName)
                    .toString());
            return false;
        }
    }

    private String readDefaultValue(final ResultSet remoteColumn) {
        try {
            if (remoteColumn.getString(DEFAULT_VALUE_COLUMN) != null) {
                return remoteColumn.getString(DEFAULT_VALUE_COLUMN);
            } else {
                return "";
            }
        } catch (final SQLException exception) {
            return "";
        }
    }

    private String readComment(final ResultSet remoteColumn) {
        try {
            final String comment = remoteColumn.getString(REMARKS_COLUMN);
            if ((comment != null) && !comment.isEmpty()) {
                return comment;
            } else {
                return "";
            }
        } catch (final SQLException exception) {
            return "";
        }
    }

    private String readColumnTypeName(final ResultSet remoteColumn) throws SQLException {
        final String columnTypeName = readTypeName(remoteColumn);
        return (columnTypeName == null) ? "" : columnTypeName;
    }

    /**
     * Read the column name form result set.
     *
     * @param columns column result set
     * @return column name
     * @throws SQLException if read fails
     */
    protected String readColumnName(final ResultSet columns) throws SQLException {
        return this.identifierConverter.convert(columns.getString(NAME_COLUMN));
    }

    @Override
    public DataType mapJdbcType(final JDBCTypeDescription jdbcTypeDescription) {
        switch (jdbcTypeDescription.getJdbcType()) {
        case Types.TINYINT:
        case Types.SMALLINT:
            return convertSmallInteger(jdbcTypeDescription.getPrecisionOrSize());
        case Types.INTEGER:
            return convertInteger(jdbcTypeDescription.getPrecisionOrSize());
        case Types.BIGINT:
            return convertBigInteger(jdbcTypeDescription.getPrecisionOrSize());
        case Types.DECIMAL:
            return convertDecimal(jdbcTypeDescription.getPrecisionOrSize(), jdbcTypeDescription.getDecimalScale());
        case Types.REAL:
        case Types.FLOAT:
        case Types.DOUBLE:
            return DataType.createDouble();
        case Types.VARCHAR:
        case Types.NVARCHAR:
        case Types.LONGVARCHAR:
        case Types.LONGNVARCHAR:
            return convertVarChar(jdbcTypeDescription.getPrecisionOrSize());
        case Types.CHAR:
        case Types.NCHAR:
            return convertChar(jdbcTypeDescription.getPrecisionOrSize());
        case Types.DATE:
            return DataType.createDate();
        case Types.TIMESTAMP:
            return convertTimestamp(jdbcTypeDescription.getDecimalScale());
        case Types.BIT:
        case Types.BOOLEAN:
            return DataType.createBool();
        case Types.TIME:
        case Types.TIMESTAMP_WITH_TIMEZONE:
            return DataType.createVarChar(100, UTF8);
        case Types.NUMERIC:
            return fallBackToMaximumSizeVarChar();
        default:
            LOGGER.finer("Found unsupported type: " + jdbcTypeDescription.getJdbcType());
            return DataType.createUnsupported();
        }
    }

    private static DataType convertSmallInteger(final int jdbcPrecision) {
        final int precision = jdbcPrecision == 0 ? 9 : jdbcPrecision;
        return DataType.createDecimal(precision, 0);
    }

    private static DataType convertInteger(final int jdbcPrecision) {
        if (jdbcPrecision <= DataType.MAX_EXASOL_DECIMAL_PRECISION) {
            final int precision = jdbcPrecision == 0 ? 18 : jdbcPrecision;
            return DataType.createDecimal(precision, 0);
        } else {
            return fallBackToMaximumSizeVarChar();
        }
    }

    private static DataType convertBigInteger(final int jdbcPrecision) {
        if (jdbcPrecision <= DataType.MAX_EXASOL_DECIMAL_PRECISION) {
            final int precision = jdbcPrecision == 0 ? 36 : jdbcPrecision;
            return DataType.createDecimal(precision, 0);
        } else {
            return fallBackToMaximumSizeVarChar();
        }
    }

    /**
     * Build a data type for a decimal value.
     *
     * @param jdbcPrecision precision
     * @param scale         scale
     * @return built data type
     */
    protected DataType convertDecimal(final int jdbcPrecision, final int scale) {
        if (jdbcPrecision <= DataType.MAX_EXASOL_DECIMAL_PRECISION) {
            return DataType.createDecimal(jdbcPrecision, scale);
        } else {
            return fallBackToMaximumSizeVarChar();
        }
    }

    private static DataType fallBackToMaximumSizeVarChar() {
        return DataType.createMaximumSizeVarChar(UTF8);
    }

    private static DataType convertVarChar(final int size) {
        final DataType.ExaCharset charset = UTF8;
        if (size <= DataType.MAX_EXASOL_VARCHAR_SIZE) {
            final int precision = size == 0 ? DataType.MAX_EXASOL_VARCHAR_SIZE : size;
            return DataType.createVarChar(precision, charset);
        } else {
            return DataType.createVarChar(DataType.MAX_EXASOL_VARCHAR_SIZE, charset);
        }
    }

    private static DataType convertChar(final int size) {
        final DataType.ExaCharset charset = UTF8;
        if (size <= DataType.MAX_EXASOL_CHAR_SIZE) {
            return DataType.createChar(size, charset);
        } else {
            if (size <= DataType.MAX_EXASOL_VARCHAR_SIZE) {
                return DataType.createVarChar(size, charset);
            } else {
                return DataType.createVarChar(DataType.MAX_EXASOL_VARCHAR_SIZE, charset);
            }
        }
    }

    private DataType convertTimestamp(final int decimalScale) {
        if (supportsTimestampsWithNanoPrecision()) {
            final int fractionalPrecision = Math.min(decimalScale, 9);
            return DataType.createTimestamp(false, fractionalPrecision);
        }
        return DataType.createTimestamp(false, 3);
    }

    /**
     * Convert the column name.
     *
     * @param columnName column name
     * @return column name
     */
    protected String mapColumnName(final String columnName) {
        return columnName;
    }

    /**
     * Map a <code>NUMERIC</code> column to an Exasol <code>DECIMAL</code>
     * <p>
     * If the precision of the remote column exceeds the maximum precision of an Exasol <code>DECIMAL</code>, the column
     * is mapped to an Exasol <code>DOUBLE</code> instead.
     *
     * @param jdbcTypeDescription parameter object describing the type from the JDBC perspective
     * @return Exasol <code>DECIMAL</code> if precision is less than or equal maximum precision, <code>DOUBLE</code>
     *         otherwise.
     */
    protected DataType mapJdbcTypeNumericToDecimalWithFallbackToDouble(final JDBCTypeDescription jdbcTypeDescription) {
        final int decimalPrec = jdbcTypeDescription.getPrecisionOrSize();
        final int decimalScale = jdbcTypeDescription.getDecimalScale();
        if (decimalPrec <= DataType.MAX_EXASOL_DECIMAL_PRECISION) {
            return DataType.createDecimal(decimalPrec, decimalScale);
        } else {
            return DataType.createDouble();
        }
    }

    /**
     * Parse a number type property.
     *
     * @param property formatted string: {@code <precision>.<scale>}
     * @return data type
     */
    protected DataType getNumberTypeFromProperty(final String property) {
        final String precisionAndScale = this.properties.get(property);
        final Matcher matcher = NUMBER_TYPE_PATTERN.matcher(precisionAndScale);
        if (matcher.matches()) {
            final int precision = Integer.parseInt(matcher.group(1));
            final int scale = Integer.parseInt(matcher.group(2));
            return DataType.createDecimal(precision, scale);
        } else {
            throw new IllegalArgumentException(ExaError.messageBuilder("E-VSCJDBC-2").message(
                    "Unable to parse adapter property {{property|uq}} value {{precisionAndScale}} into a number precision "
                            + "and scale. The required format is '<precision>.<scale>', where both are integer numbers.",
                    property, precisionAndScale).toString());
        }
    }
}