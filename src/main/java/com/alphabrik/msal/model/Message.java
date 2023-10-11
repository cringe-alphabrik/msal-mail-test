package com.alphabrik.msal.model;

import java.time.Instant;

public record Message(
    String id,
    String internetMessageId,
    From from,
    String subject,
    String bodyPreview,
    Instant receivedDateTime,
    boolean isRead
) {

}
