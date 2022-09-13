package com.exasol.adapter.dialects.rewriting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.exasol.ExaConnectionAccessException;
import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.jdbc.BaseConnectionDefinitionBuilder;
import com.exasol.adapter.jdbc.RemoteMetadataReader;

class AbstractQueryRewriterTest {
    @Test
    void testGetConnectionInformationThrowsException() throws ExaConnectionAccessException {
        final DummyQueryRewriter dummyQueryRewriter = new DummyQueryRewriter(null, null);
        final ExaMetadata exaMetadataMock = Mockito.mock(ExaMetadata.class);
        when(exaMetadataMock.getConnection("my_connection")).thenThrow(ExaConnectionAccessException.class);
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> dummyQueryRewriter.getConnectionInformation(exaMetadataMock,
                        new AdapterProperties(Map.of("CONNECTION_NAME", "my_connection"))));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-8"));
    }

    static class DummyQueryRewriter extends AbstractQueryRewriter {
        /**
         * Create a new instance of a {@link AbstractQueryRewriter}.
         *
         * @param dialect              dialect
         * @param remoteMetadataReader remote metadata reader
         */
        protected DummyQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader) {
            super(dialect, remoteMetadataReader, new BaseConnectionDefinitionBuilder());
        }

        @Override
        protected String generateImportStatement(final String connectionDefinition, final String pushdownQuery) {
            return "";
        }
    }
}
