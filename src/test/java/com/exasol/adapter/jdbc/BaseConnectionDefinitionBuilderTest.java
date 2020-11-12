package com.exasol.adapter.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.exasol.ExaConnectionInformation;

class BaseConnectionDefinitionBuilderTest extends AbstractConnectionDefinitionBuilderTestBase {
    @BeforeEach
    void beforeEach() {
        this.exaConnectionInformation = mock(ExaConnectionInformation.class);
        this.rawProperties = new HashMap<>();
    }

    @Override
    protected ConnectionDefinitionBuilder createConnectionBuilderUnderTest() {
        return new BaseConnectionDefinitionBuilder();
    }

    @Test
    void testBuildConnectionDefinitionForJDBCImportWithConnectionNameGiven() {
        mockExasolNamedConnection();
        setConnectionNameProperty();
        assertThat(calculateConnectionDefinition(), equalTo("AT " + CONNECTION_NAME));
    }

    @Test
    void testBuildConnectionDefinitionWithoutConnectionInformationThrowsException() {
        assertIllegalPropertiesThrowsException(Collections.emptyMap());
    }

    @Test
    void testGetConnectionDefinition() {
        final BaseConnectionDefinitionBuilder baseConnectionDefinitionBuilder = new BaseConnectionDefinitionBuilder();
        assertThat(baseConnectionDefinitionBuilder.getConnectionDefinition("localhost:6666", "username", "password"),
                equalTo("AT 'localhost:6666' USER 'username' IDENTIFIED BY 'password'"));
    }

    @Test
    void testGetConnectionDefinitionQuoting() {
        final BaseConnectionDefinitionBuilder baseConnectionDefinitionBuilder = new BaseConnectionDefinitionBuilder();
        assertThat(
                baseConnectionDefinitionBuilder.getConnectionDefinition("localhost:6666 \" ' ", "username \" ' ",
                        "password \" ' "),
                equalTo("AT 'localhost:6666 \" '' ' USER 'username \" '' ' IDENTIFIED BY 'password \" '' '"));
    }
}