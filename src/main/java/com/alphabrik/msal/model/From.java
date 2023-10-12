package com.alphabrik.msal.model;

record From(
    EmailAddress emailAddress
) {

        @Override
        public String toString() {
            return emailAddress.toString();
        }
}
