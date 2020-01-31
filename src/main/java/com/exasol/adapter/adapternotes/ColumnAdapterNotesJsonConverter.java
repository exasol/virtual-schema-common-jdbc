package com.exasol.adapter.adapternotes;

import javax.json.*;

import com.exasol.adapter.AdapterException;
import com.exasol.utils.JsonHelper;

/**
 * Converts column adapter Notes into JSON format and back.
 */
public final class ColumnAdapterNotesJsonConverter {
    private static final ColumnAdapterNotesJsonConverter COLUMN_ADAPTER_NOTES_JSON_CONVERTER = new ColumnAdapterNotesJsonConverter();

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
        final JsonBuilderFactory factory = JsonHelper.getBuilderFactory();
        final JsonObjectBuilder builder = factory.createObjectBuilder().add("jdbcDataType",
                columnAdapterNotes.getJdbcDataType());
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
            throw new AdapterException("Adapter notes for column " + columnName + " are empty or NULL. " //
                    + "Please refresh the virtual schema.");
        }
        final JsonObject root;
        try {
            root = JsonHelper.getJsonObject(adapterNotes);
        } catch (final Exception exception) {
            throw new AdapterException("Could not parse the column adapter notes of column \"" + columnName + "\"." //
                    + "Please refresh the virtual schema.", exception);
        }
        return new ColumnAdapterNotes(root.getInt("jdbcDataType"));
    }
}