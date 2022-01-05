package com.exasol.adapter.dialects;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class ImportTypeTest {

    @Test
    void testEnumValues() {
        final List<String> values = Arrays.stream(ImportType.values()).map(Enum::name).collect(Collectors.toList());
        // be careful when changing since it's used in dialects
        assertThat(values, Matchers.containsInAnyOrder("JDBC", "EXA", "LOCAL", "ORA"));
    }
}