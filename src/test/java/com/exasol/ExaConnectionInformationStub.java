package com.exasol;

public class ExaConnectionInformationStub implements ExaConnectionInformation {
    private final String user;
    private final String password;
    private final String address;
    private final ConnectionType type;

    public ExaConnectionInformationStub(final Builder builder) {
        this.user = builder.user;
        this.password = builder.password;
        this.address = builder.address;
        this.type = builder.type;
    }

    @Override
    public ConnectionType getType() {
        return this.type;
    }

    @Override
    public String getAddress() {
        return this.address;
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String user;
        private String password;
        private String address;
        public ConnectionType type;

        public Builder user(final String user) {
            this.user = user;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder address(final String address) {
            this.address = address;
            return this;
        }

        public Builder type(final ConnectionType type) {
            this.type = type;
            return this;
        }

        public ExaConnectionInformation build() {
            return new ExaConnectionInformationStub(this);
        }
    }
}