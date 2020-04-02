package com.exasol;

import java.math.BigInteger;

public class ExaMetadataStub implements ExaMetadata {
    private final ExaConnectionInformation exaConnectionInformation;

    private ExaMetadataStub(final Builder builder) {
        this.exaConnectionInformation = builder.exaConnectionInformation;
    }

    @Override
    public String getDatabaseName() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getDatabaseVersion() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getScriptLanguage() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getScriptName() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getScriptSchema() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getCurrentUser() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getScopeUser() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getCurrentSchema() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getScriptCode() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getSessionId() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getStatementId() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getNodeCount() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getNodeId() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getVmId() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public BigInteger getMemoryLimit() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getInputType() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getInputColumnCount() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getInputColumnName(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Class<?> getInputColumnType(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getInputColumnSqlType(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getInputColumnPrecision(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getInputColumnScale(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getInputColumnLength(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getOutputType() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getOutputColumnCount() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getOutputColumnName(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Class<?> getOutputColumnType(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getOutputColumnSqlType(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getOutputColumnPrecision(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getOutputColumnScale(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getOutputColumnLength(final int column) throws ExaIterationException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Class<?> importScript(final String name) throws ExaCompilationException, ClassNotFoundException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public ExaConnectionInformation getConnection(final String name) throws ExaConnectionAccessException {
        return this.exaConnectionInformation;
    }

    static public Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public ExaConnectionInformation exaConnectionInformation;

        public Builder exaConnectionInformation(final ExaConnectionInformation exaConnectionInformation) {
            this.exaConnectionInformation = exaConnectionInformation;
            return this;
        }

        public ExaMetadataStub build() {
            return new ExaMetadataStub(this);
        }
    }
}