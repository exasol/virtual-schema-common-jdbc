package com.exasol.adapter.adapternotes;

import java.io.StringReader;
import java.util.Collections;

import com.exasol.adapter.AdapterException;
import com.exasol.errorreporting.ExaError;

import jakarta.json.*;

/**
 * Converts column adapter Notes into JSON format and back.
 */
public final class ColumnAdapterNotesJsonConverter {
    private static final String MITIGATION_REFRESH = "Please refresh the virtual schema.";
    /** Key for the jdbc type in the adapter notes */
    protected static final String JDBC_DATA_TYPE = "jdbcDataType";
    /** Key for the type name in the adapter notes */
    protected static final String TYPE_NAME = "typeName";
    private static final ColumnAdapterNotesJsonConverter COLUMN_ADAPTER_NOTES_JSON_CONVERTER = new ColumnAdapterNotesJsonConverter();
    private final JsonBuilderFactory factory = Json.createBuilderFactory(Collections.emptyMap());

    /**
     * Returns instance of {@link ColumnAdapterNotesJsonConverter} singleton class.
     *
     * @return {@link ColumnAdapterNotesJsonConverter} instance
     */
    public static ColumnAdapterNotesJsonConverter getInstance() {
        return COLUMN_ADAPTER_NOTES_JSON_CONVERTER;
    }

    private ColumnAdapterNotesJsonConverter() {
        // intentionally left blank
    }

    /**
     * Converts column adapter notes into a JSON format.
     *
     * @param columnAdapterNotes column adapter notes to be converted
     * @return string representation of a JSON Object
     */
    public String convertToJson(final ColumnAdapterNotes columnAdapterNotes) {
        final JsonObjectBuilder builder = this.factory.createObjectBuilder() //
                .add(JDBC_DATA_TYPE, columnAdapterNotes.getJdbcDataType());
        final String typeName = columnAdapterNotes.getTypeName();
        if (typeName != null) {
            builder.add(TYPE_NAME, typeName);
        }
        return builder.build().toString();
    }

    /**
     * Converts JSON representation of column adapter notes into instance of {@link ColumnAdapterNotes} class.
     *
     * @param adapterNotes JSON representation of schema adapter notes
     * @param columnName   name of the column
     * @return instance of {@link ColumnAdapterNotes}
     * @throws AdapterException if the adapter notes are missing or cannot be parsed
     */
    public ColumnAdapterNotes convertFromJsonToColumnAdapterNotes(final String adapterNotes, final String columnName)
            throws AdapterException {
        if ((adapterNotes == null) || adapterNotes.isEmpty()) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-3")
                    .message("Adapter notes for column \"{{columnName|uq}}\" are empty or NULL.", columnName)
                    .mitigation(MITIGATION_REFRESH).toString());
        }
        final JsonObject root;
        try (final JsonReader jr = Json.createReader(new StringReader(adapterNotes))) {
            root = jr.readObject();
        } catch (final JsonException exception) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-4")
                    .message("Could not parse the column adapter notes of column {{columnName}}. Error: {{errorMessage}}", columnName, exception.getMessage())
                    .mitigation(MITIGATION_REFRESH).toString());
        }
        if (!root.containsKey(JDBC_DATA_TYPE) || root.get(JDBC_DATA_TYPE).getValueType() != JsonValue.ValueType.NUMBER) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-48")
                    .message("The column adapter notes of column {{columnName}} are missing mandatory field 'jdbcDataType' or it is not a number.", columnName)
                    .mitigation(MITIGATION_REFRESH).toString());
        }
        final int jdbcDataType = root.getInt(JDBC_DATA_TYPE);
        final ColumnAdapterNotes.Builder builder = ColumnAdapterNotes.builder()
                .jdbcDataType(jdbcDataType);
        if (root.containsKey(TYPE_NAME) && !root.isNull(TYPE_NAME)) {
            if (root.get(TYPE_NAME).getValueType() != JsonValue.ValueType.STRING) {
                throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-50")
                        .message("Optional field 'typeName' in column adapter notes of column {{columnName}} must be a string.", columnName)
                        .mitigation(MITIGATION_REFRESH).toString());
            }
            builder.typeName(root.getString(TYPE_NAME));
        }
        return builder.build();
    }
}
