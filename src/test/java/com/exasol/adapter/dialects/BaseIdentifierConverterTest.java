package com.exasol.adapter.dialects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BaseIdentifierConverterTest {
    final IdentifierConverter identifierConverter = new BaseIdentifierConverter(
            IdentifierCaseHandling.INTERPRET_AS_UPPER, IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE);

    @CsvSource({ "INTERPRET_AS_LOWER, INTERPRET_AS_LOWER, true", //
            "INTERPRET_AS_LOWER, INTERPRET_AS_UPPER, false", //
            "INTERPRET_AS_LOWER, INTERPRET_CASE_SENSITIVE, false", //
            "INTERPRET_AS_UPPER, INTERPRET_AS_UPPER, true", //
            "INTERPRET_AS_UPPER, INTERPRET_AS_LOWER, false", //
            "INTERPRET_AS_UPPER, INTERPRET_CASE_SENSITIVE, false", //
            "INTERPRET_CASE_SENSITIVE, INTERPRET_AS_LOWER, false", //
            "INTERPRET_CASE_SENSITIVE, INTERPRET_AS_UPPER, false", //
            "INTERPRET_CASE_SENSITIVE, INTERPRET_CASE_SENSITIVE, false" })
    @ParameterizedTest
    void testConvert(final IdentifierCaseHandling unquotedIdentifierHandling,
            final IdentifierCaseHandling quotedIdentifierHandling, final boolean resultShouldBeUpperCase) {
        final IdentifierConverter converter = new BaseIdentifierConverter(unquotedIdentifierHandling,
                quotedIdentifierHandling);
        assertThat(converter.convert("text"), equalTo(resultShouldBeUpperCase ? "TEXT" : "text"));
    }

    @Test
    void testConvertUsesLocaleIndependentUpperCase() {
        final Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            final IdentifierConverter converter = new BaseIdentifierConverter(
                    IdentifierCaseHandling.INTERPRET_AS_UPPER, IdentifierCaseHandling.INTERPRET_AS_UPPER);
            assertThat(converter.convert("i"), equalTo("I"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void testGetUnquotedIdentifierHandling() {
        assertThat(this.identifierConverter.getUnquotedIdentifierHandling(),
                equalTo(IdentifierCaseHandling.INTERPRET_AS_UPPER));
    }

    @Test
    void testGetQuotedIdentifierHandling() {
        assertThat(this.identifierConverter.getQuotedIdentifierHandling(),
                equalTo(IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE));
    }

    @Test
    void testCreateDefault() {
        final IdentifierConverter converter = BaseIdentifierConverter.createDefault();
        assertAll(
                () -> assertThat(converter.getQuotedIdentifierHandling(),
                        equalTo(IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE)),
                () -> assertThat(converter.getUnquotedIdentifierHandling(),
                        equalTo(IdentifierCaseHandling.INTERPRET_AS_UPPER)));
    }
}
