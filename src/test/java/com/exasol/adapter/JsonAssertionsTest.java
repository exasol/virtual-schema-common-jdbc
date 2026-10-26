package com.exasol.adapter;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class JsonAssertionsTest {
    @Test
    void matchesNestedObjectsAndUnorderedArrays() {
        JsonAssertions.assertJsonEquals("{\"items\":[{\"id\":1},{\"id\":2}]}",
                "{\"items\":[{\"id\":2,\"extra\":true},{\"id\":1}],\"other\":null}");
    }

    @Test
    void rejectsReusingAnArrayElement() {
        assertThrows(AssertionFailedError.class,
                () -> JsonAssertions.assertJsonEquals("[{\"id\":1},{\"id\":1}]", "[{\"id\":1},{\"id\":2}]"));
    }
}
