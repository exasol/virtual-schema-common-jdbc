package com.exasol.adapter.dialects.rewriting;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.sql.*;

import com.exasol.*;
import com.exasol.adapter.sql.SqlStatement;

public abstract class AbstractQueryRewriterTestBase {
    protected static final String CONNECTION_NAME = "the_connection";
    private static final String CONNECTION_USER = "connection_user";
    private static final String CONNECTION_PW = "connection_secret";
    private static final String CONNECTION_ADDRESS = "connection_address";
    private static final ExaConnectionInformation EXA_CONNECTION_INFORMATION = ExaConnectionInformationStub.builder() //
            .user(CONNECTION_USER) //
            .password(CONNECTION_PW) //
            .address(CONNECTION_ADDRESS) //
            .build();
    protected static final ExaMetadata EXA_METADATA = ExaMetadataStub.builder()
            .exaConnectionInformation(EXA_CONNECTION_INFORMATION) //
            .build();
    protected SqlStatement statement;

    protected Connection mockConnection() throws SQLException {
        final ResultSetMetaData metadataMock = mock(ResultSetMetaData.class);
        lenient().when(metadataMock.getColumnCount()).thenReturn(1);
        lenient().when(metadataMock.getColumnType(1)).thenReturn(Types.INTEGER);
        final PreparedStatement statementMock = mock(PreparedStatement.class);
        lenient().when(statementMock.getMetaData()).thenReturn(metadataMock);
        final Connection connectionMock = mock(Connection.class);
        lenient().when(connectionMock.prepareStatement(any())).thenReturn(statementMock);
        return connectionMock;
    }
}
