package com.alphabrik.msal.model;

record From(
    EmailAddress emailAddress
) {

    @Override
    public String toString() {
        return String.format("%s <%s>", emailAddress.name(), emailAddress.address());
    }
}
