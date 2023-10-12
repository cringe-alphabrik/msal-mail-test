package com.alphabrik.msal.model;

record EmailAddress(
    String name,
    String address
) {

    @Override
    public String toString() {
        return name + " <" + address + ">";
    }
}
