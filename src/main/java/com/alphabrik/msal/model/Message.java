package com.alphabrik.msal.model;

import java.time.Instant;

public record Message(
    String id,
    From from,
    String subject,
    Instant sentDateTime,
    Instant receivedDateTime,
    boolean isRead,
    boolean hasAttachments
) {

    @Override
    public String toString() {
        return id +
               " from:" + from +
               " subject:" + subject +
               " received:" + receivedDateTime +
               " read:" + isRead;
    }
}
