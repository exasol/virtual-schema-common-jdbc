package com.exasol.adapter.adapternotes;

import java.io.StringReader;
import java.util.Collections;

import com.exasol.adapter.AdapterException;
import com.exasol.errorreporting.ExaError;

import jakarta.json.*;

/**
 * Converts schema adapter Notes into JSON format and back.
 */
public final class SchemaAdapterNotesJsonConverter {
    private static final String CATALOG_SEPARATOR = "catalogSeparator";
    private static final String IDENTIFIER_QUOTE_STRING = "identifierQuoteString";
    private static final String STORES_LOWER_CASE_IDENTIFIERS = "storesLowerCaseIdentifiers";
    private static final String STORES_UPPER_CASE_IDENTIFIERS = "storesUpperCaseIdentifiers";
    private static final String STORES_MIXED_CASE_IDENTIFIERS = "storesMixedCaseIdentifiers";
    private static final String SUPPORTS_MIXED_CASE_IDENTIFIERS = "supportsMixedCaseIdentifiers";
    private static final String STORES_LOWER_CASE_QUOTED_IDENTIFIERS = "storesLowerCaseQuotedIdentifiers";
    private static final String STORES_UPPER_CASE_QUOTED_IDENTIFIERS = "storesUpperCaseQuotedIdentifiers";
    private static final String STORES_MIXED_CASE_QUOTED_IDENTIFIERS = "storesMixedCaseQuotedIdentifiers";
    private static final String SUPPORTS_MIXED_CASE_QUOTED_IDENTIFIERS = "supportsMixedCaseQuotedIdentifiers";
    private static final String NULLS_ARE_SORTED_AT_END = "areNullsSortedAtEnd";
    private static final String NULLS_ARE_SORTED_AT_START = "areNullsSortedAtStart";
    private static final String NULLS_ARE_SORTED_HIGH = "areNullsSortedHigh";
    private static final String NULLS_ARE_SORTED_LOW = "areNullsSortedLow";
    private static final SchemaAdapterNotesJsonConverter SCHEMA_ADAPTER_NOTES_JSON_CONVERTER = new SchemaAdapterNotesJsonConverter();
    private final JsonBuilderFactory factory = Json.createBuilderFactory(Collections.emptyMap());

    /**
     * Returns instance of {@link SchemaAdapterNotesJsonConverter} singleton class.
     *
     * @return {@link SchemaAdapterNotesJsonConverter} instance
     */
    public static SchemaAdapterNotesJsonConverter getInstance() {
        return SCHEMA_ADAPTER_NOTES_JSON_CONVERTER;
    }

    private SchemaAdapterNotesJsonConverter() {
        // intentionally left blank
    }

    /**
     * Converts schema adapter notes into a JSON format.
     *
     * @param schemaAdapterNotes schema adapter notes to be converted
     * @return string representation of a JSON Object
     */
    public String convertToJson(final SchemaAdapterNotes schemaAdapterNotes) {
        final JsonObjectBuilder builder = this.factory.createObjectBuilder()
                .add(CATALOG_SEPARATOR, schemaAdapterNotes.getCatalogSeparator())
                .add(IDENTIFIER_QUOTE_STRING, schemaAdapterNotes.getIdentifierQuoteString())
                .add(STORES_LOWER_CASE_IDENTIFIERS, schemaAdapterNotes.storesLowerCaseIdentifiers())
                .add(STORES_UPPER_CASE_IDENTIFIERS, schemaAdapterNotes.storesUpperCaseIdentifiers())
                .add(STORES_MIXED_CASE_IDENTIFIERS, schemaAdapterNotes.storesMixedCaseIdentifiers())
                .add(SUPPORTS_MIXED_CASE_IDENTIFIERS, schemaAdapterNotes.supportsMixedCaseIdentifiers())
                .add(STORES_LOWER_CASE_QUOTED_IDENTIFIERS, schemaAdapterNotes.storesLowerCaseQuotedIdentifiers())
                .add(STORES_UPPER_CASE_QUOTED_IDENTIFIERS, schemaAdapterNotes.storesUpperCaseQuotedIdentifiers())
                .add(STORES_MIXED_CASE_QUOTED_IDENTIFIERS, schemaAdapterNotes.storesMixedCaseQuotedIdentifiers())
                .add(SUPPORTS_MIXED_CASE_QUOTED_IDENTIFIERS, schemaAdapterNotes.supportsMixedCaseQuotedIdentifiers())
                .add(NULLS_ARE_SORTED_AT_END, schemaAdapterNotes.areNullsSortedAtEnd())
                .add(NULLS_ARE_SORTED_AT_START, schemaAdapterNotes.areNullsSortedAtStart())
                .add(NULLS_ARE_SORTED_HIGH, schemaAdapterNotes.areNullsSortedHigh())
                .add(NULLS_ARE_SORTED_LOW, schemaAdapterNotes.areNullsSortedLow());
        return builder.build().toString();
    }

    private static void checkKey(final JsonObject root, final String key, final String schemaName)
            throws AdapterException {
        if (!root.containsKey(key)) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-7")
                    .message("Adapter notes of virtual schema \"{{schemaName|uq}}\" don't have the key {{key}}.",
                            schemaName, key)
                    .mitigation("Please refresh the virtual schema").toString());
        }
    }

    /**
     * Converts JSON representation of schema adapter notes into instance of {@link SchemaAdapterNotes} class.
     *
     * @param adapterNotes JSON representation of schema adapter notes
     * @param schemaName   name of virtual schema
     * @return instance of {@link SchemaAdapterNotes}
     * @throws AdapterException if the adapter notes are missing or cannot be parsed
     */
    public SchemaAdapterNotes convertFromJsonToSchemaAdapterNotes(final String adapterNotes, final String schemaName)
            throws AdapterException {
        if ((adapterNotes == null) || adapterNotes.isEmpty()) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-5")
                    .message("Adapter notes for virtual schema \"{{schemaName|uq}}\" are empty or NULL.", schemaName)
                    .mitigation("Please refresh the virtual schema.").toString());
        }
        final JsonObject root;
        try (final JsonReader jr = Json.createReader(new StringReader(adapterNotes))) {
            root = jr.readObject();
        } catch (final RuntimeException exception) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-6")
                    .message("Could not parse the schema adapter notes of virtual schema \"{{schemaName|uq}}\".",
                            schemaName)
                    .mitigation("Please refresh the virtual schema.").toString(), exception);
        }
        checkKey(root, CATALOG_SEPARATOR, schemaName);
        checkKey(root, IDENTIFIER_QUOTE_STRING, schemaName);
        checkKey(root, STORES_LOWER_CASE_IDENTIFIERS, schemaName);
        checkKey(root, STORES_UPPER_CASE_IDENTIFIERS, schemaName);
        checkKey(root, STORES_MIXED_CASE_IDENTIFIERS, schemaName);
        checkKey(root, SUPPORTS_MIXED_CASE_IDENTIFIERS, schemaName);
        checkKey(root, STORES_LOWER_CASE_QUOTED_IDENTIFIERS, schemaName);
        checkKey(root, STORES_UPPER_CASE_QUOTED_IDENTIFIERS, schemaName);
        checkKey(root, STORES_MIXED_CASE_QUOTED_IDENTIFIERS, schemaName);
        checkKey(root, SUPPORTS_MIXED_CASE_QUOTED_IDENTIFIERS, schemaName);
        checkKey(root, NULLS_ARE_SORTED_AT_END, schemaName);
        checkKey(root, NULLS_ARE_SORTED_AT_START, schemaName);
        checkKey(root, NULLS_ARE_SORTED_HIGH, schemaName);
        checkKey(root, NULLS_ARE_SORTED_LOW, schemaName);
        return SchemaAdapterNotes.builder() //
                .catalogSeparator(root.getString(CATALOG_SEPARATOR)) //
                .identifierQuoteString(root.getString(IDENTIFIER_QUOTE_STRING)) //
                .storesLowerCaseIdentifiers(root.getBoolean(STORES_LOWER_CASE_IDENTIFIERS)) //
                .storesUpperCaseIdentifiers(root.getBoolean(STORES_UPPER_CASE_IDENTIFIERS)) //
                .storesMixedCaseIdentifiers(root.getBoolean(STORES_MIXED_CASE_IDENTIFIERS)) //
                .supportsMixedCaseIdentifiers(root.getBoolean(SUPPORTS_MIXED_CASE_IDENTIFIERS)) //
                .storesLowerCaseQuotedIdentifiers(root.getBoolean(STORES_LOWER_CASE_QUOTED_IDENTIFIERS)) //
                .storesUpperCaseQuotedIdentifiers(root.getBoolean(STORES_UPPER_CASE_QUOTED_IDENTIFIERS)) //
                .storesMixedCaseQuotedIdentifiers(root.getBoolean(STORES_MIXED_CASE_QUOTED_IDENTIFIERS)) //
                .supportsMixedCaseQuotedIdentifiers(root.getBoolean(SUPPORTS_MIXED_CASE_QUOTED_IDENTIFIERS))//
                .areNullsSortedAtEnd(root.getBoolean(NULLS_ARE_SORTED_AT_END)) //
                .areNullsSortedAtStart(root.getBoolean(NULLS_ARE_SORTED_AT_START)) //
                .areNullsSortedHigh(root.getBoolean(NULLS_ARE_SORTED_HIGH)) //
                .areNullsSortedLow(root.getBoolean(NULLS_ARE_SORTED_LOW)) //
                .build();
    }
}