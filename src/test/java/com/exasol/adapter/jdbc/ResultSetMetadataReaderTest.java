package com.exasol.adapter.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.*;

import com.exasol.ExaMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseIdentifierConverter;

@ExtendWith(MockitoExtension.class)
class ResultSetMetadataReaderTest {
    @Mock
    private ResultSetMetaData resultSetMetadataMock;
    @Mock
    private Connection connectionMock;
    @Mock
    private ExaMetadata exaMetadataMock;
    @Mock
    private PreparedStatement statementMock;

    @Test
    void testDescribeColumn() throws SQLException {
        when(this.resultSetMetadataMock.getColumnCount()).thenReturn(2);
        when(this.resultSetMetadataMock.getColumnType(1)).thenReturn(Types.BOOLEAN);
        when(this.resultSetMetadataMock.getColumnType(2)).thenReturn(Types.VARCHAR);
        when(this.resultSetMetadataMock.getPrecision(1)).thenReturn(0);
        when(this.resultSetMetadataMock.getPrecision(2)).thenReturn(20);
        final String columnDescription = "c1 BOOLEAN, c2 VARCHAR(20) UTF8";
        assertThat(getReader().describeColumns("irrelevant"), equalTo(columnDescription));
    }

    public ResultSetMetadataReader getReader() throws SQLException {
        when(this.exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
        when(this.statementMock.getMetaData()).thenReturn(this.resultSetMetadataMock);
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.statementMock);
        final ColumnMetadataReader columnMetadataReader = new BaseColumnMetadataReader(this.connectionMock,
                AdapterProperties.emptyProperties(), exaMetadataMock, BaseIdentifierConverter.createDefault());
        return new ResultSetMetadataReader(this.connectionMock, columnMetadataReader);
    }

    @Test
    void testDescribeColumnThrowsExceptionIfUnsupportedColumnContained() throws SQLException {
        when(this.resultSetMetadataMock.getColumnCount()).thenReturn(4);
        when(this.resultSetMetadataMock.getColumnType(1)).thenReturn(Types.BOOLEAN);
        when(this.resultSetMetadataMock.getColumnType(2)).thenReturn(Types.BLOB);
        when(this.resultSetMetadataMock.getColumnType(3)).thenReturn(Types.DATE);
        when(this.resultSetMetadataMock.getColumnType(4)).thenReturn(Types.BLOB);
        final ResultSetMetadataReader reader = getReader();
        final RemoteMetadataReaderException thrown = assertThrows(RemoteMetadataReaderException.class,
                () -> reader.describeColumns("FOOBAR"));
        assertThat(thrown.getMessage(),
                containsString("E-VSCJDBC-31: Unsupported data type(s) in column(s) in query: 2, 4"));
    }

    @Test
    void testEmptyMetadata() throws SQLException {
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.statementMock);
        when(this.exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
        final ColumnMetadataReader columnMetadataReader = new BaseColumnMetadataReader(this.connectionMock,
                AdapterProperties.emptyProperties(), exaMetadataMock, BaseIdentifierConverter.createDefault());
        final ResultSetMetadataReader metadataReader = new ResultSetMetadataReader(this.connectionMock,
                columnMetadataReader);
        final RemoteMetadataReaderException exception = assertThrows(RemoteMetadataReaderException.class,
                () -> metadataReader.describeColumns("FOOBAR"));
        assertThat(exception.getMessage(), containsString("F-VSCJDBC-34"));
    }
}
