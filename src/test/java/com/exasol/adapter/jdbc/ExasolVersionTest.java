package com.exasol.adapter.jdbc;

import com.exasol.ExaMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExasolVersionTest {

    @Mock
    private ExaMetadata exaMetadata;

    @Test
    void testParseValidVersion() {
        when(exaMetadata.getDatabaseVersion()).thenReturn("7.1.12");

        final ExasolVersion testee = ExasolVersion.parse(exaMetadata);

        assertTrue(testee.atLeast(7, 1));
        assertFalse(testee.atLeast(7, 2));
        assertTrue(testee.atLeast(6, 0));
    }

    @Test
    void testParseValidVersionWithoutPatch() {
        when(exaMetadata.getDatabaseVersion()).thenReturn("8.32");

        final ExasolVersion testee = ExasolVersion.parse(exaMetadata);

        assertTrue(testee.atLeast(8, 32));
        assertFalse(testee.atLeast(8, 33));
    }

    @Test
    void testParseInvalidVersionReturnsZeroZero() {
        when(exaMetadata.getDatabaseVersion()).thenReturn("invalid-version");

        final ExasolVersion testee = ExasolVersion.parse(exaMetadata);

        assertFalse(testee.atLeast(1, 0));
        assertFalse(testee.atLeast(0, 1));
        assertTrue(testee.atLeast(0, 0));
    }

    @Test
    void testParseEmptyVersionReturnsZeroZero() {
        when(exaMetadata.getDatabaseVersion()).thenReturn("");

        final ExasolVersion testee = ExasolVersion.parse(exaMetadata);

        assertFalse(testee.atLeast(1, 0));
        assertTrue(testee.atLeast(0, 0));
    }

    @Test
    void testAtLeastWithExactMatch() {
        when(exaMetadata.getDatabaseVersion()).thenReturn("8.3.0");

        final ExasolVersion testee = ExasolVersion.parse(exaMetadata);
        assertTrue(testee.atLeast(8, 3));
    }

    @Test
    void testAtLeastMajorGreater() {
        when(exaMetadata.getDatabaseVersion()).thenReturn("9.0.0");

        final ExasolVersion testee = ExasolVersion.parse(exaMetadata);
        assertTrue(testee.atLeast(8, 5));
    }

    @Test
    void testAtLeastMinorGreater() {
        when(exaMetadata.getDatabaseVersion()).thenReturn("7.5.0");

        final ExasolVersion testee = ExasolVersion.parse(exaMetadata);
        assertTrue(testee.atLeast(7, 3));
        assertFalse(testee.atLeast(7, 6));
    }
}
