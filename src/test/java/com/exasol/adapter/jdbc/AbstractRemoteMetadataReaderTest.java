package com.exasol.adapter.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.exasol.adapter.AdapterProperties;

class AbstractRemoteMetadataReaderTest {
    @Test
    void testGetSchemaAdapterNotesWithSqlException() throws SQLException {
        final Connection connectionMock = mockConnectionThrowingExceptionOnGetMetadata();
        final RemoteMetadataReader reader = new DummyRemoteMetadataReader(connectionMock,
                AdapterProperties.emptyProperties());
        assertThrows(RemoteMetadataReaderException.class, () -> reader.getSchemaAdapterNotes());
    }

    private Connection mockConnectionThrowingExceptionOnGetMetadata() throws SQLException {
        final Connection connectionMock = Mockito.mock(Connection.class);
        when(connectionMock.getMetaData()).thenThrow(new SQLException("FAKE SQL exception"));
        return connectionMock;
    }

    @Test
    void testReadRemoteSchemaMetadataWithSqlException() throws SQLException {
        final Connection connectionMock = mockConnectionThrowingExceptionOnGetMetadata();
        final RemoteMetadataReader reader = new DummyRemoteMetadataReader(connectionMock,
                AdapterProperties.emptyProperties());
        assertThrows(RemoteMetadataReaderException.class, () -> reader.readRemoteSchemaMetadata());
    }

    @Test
    void testReadRemoteSchemaMetadataWithTableListAndSqlException() throws SQLException {
        final Connection connectionMock = mockConnectionThrowingExceptionOnGetMetadata();
        final RemoteMetadataReader reader = new DummyRemoteMetadataReader(connectionMock,
                AdapterProperties.emptyProperties());
        final List<String> tables = Collections.emptyList();
        final RemoteMetadataReaderException exception = assertThrows(RemoteMetadataReaderException.class,
                () -> reader.readRemoteSchemaMetadata(tables));
        assertThat(exception.getMessage(), containsString("E-VS-COM-JDBC-22"));
    }
}