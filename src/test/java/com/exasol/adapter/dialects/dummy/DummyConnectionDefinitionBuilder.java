package com.exasol.adapter.dialects.dummy;

import com.exasol.ExaConnectionInformation;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.ConnectionDefinitionBuilder;

public class DummyConnectionDefinitionBuilder implements ConnectionDefinitionBuilder {
    @Override
    public String buildConnectionDefinition(final AdapterProperties properties,
            final ExaConnectionInformation exaConnectionInformation) {
        return "MY DUMMY DEFINITION BUILDER";
    }
}
