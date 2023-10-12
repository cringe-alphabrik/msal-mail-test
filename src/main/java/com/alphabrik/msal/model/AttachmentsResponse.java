package com.alphabrik.msal.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AttachmentsResponse(
    @JsonProperty("@odata.context")
    String context,
    List<Attachment> value
) {

}
