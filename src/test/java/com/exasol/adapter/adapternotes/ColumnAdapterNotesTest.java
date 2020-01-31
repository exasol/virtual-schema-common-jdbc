package com.exasol.adapter.adapternotes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class ColumnAdapterNotesTest {
    private ColumnAdapterNotes columnAdapterNotes;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetJdbcDataType() {
        this.columnAdapterNotes = new ColumnAdapterNotes(8);
        assertThat(this.columnAdapterNotes.getJdbcDataType(), equalTo(8));
    }

    @Test
    void equalsContract() {
        EqualsVerifier.forClass(ColumnAdapterNotes.class).verify();
    }
}