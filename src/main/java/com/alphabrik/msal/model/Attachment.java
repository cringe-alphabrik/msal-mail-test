package com.alphabrik.msal.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof final Attachment that)) return false;
        return Objects.equals(type, that.type) && Objects.equals(id, that.id) && Objects.equals(
            name,
            that.name
        ) && Objects.equals(contentType, that.contentType) && Objects.equals(lastModifiedDateTime, that.lastModifiedDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id, name, contentType, lastModifiedDateTime);
    }

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
