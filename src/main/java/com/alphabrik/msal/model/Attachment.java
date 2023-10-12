package com.alphabrik.msal.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record Attachment(
    @JsonProperty("@odata.type")
    String type,
    String id,
    String name,
    String contentType,
    byte[] contentBytes,
    Instant lastModifiedDateTime
) {

    @Override
    public String toString() {
        return "Attachment{" +
               "id='" + id + '\'' +
               ", type='" + type + '\'' +
               ", name='" + name + '\'' +
               ", contentType='" + contentType + '\'' +
               ", contentBytes=[" + shorten(25) + "]" +
               '}';
    }

    private String shorten(final int maxLength) {
        return new String(contentBytes, 0, maxLength) + (contentBytes.length > maxLength ? "..." : "");
    }
}
