package com.exasol.adapter.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WildcardsTest {
    @ParameterizedTest
    @CsvSource(value = { //
            "abc, abc", //
            "a _ a, a \\_ a", //
            "a % a, a \\% a", //
            "a _ b _ c, a \\_ b \\_ c", //
            "a_b%c, a\\_b\\%c", //
    })
    void test(final String input, final String expected) {
        assertThat(Wildcards.escape(input), equalTo(expected));
    }

    @Test
    void testEmpty() {
        assertThat(Wildcards.escape(""), equalTo(""));
    }
}
