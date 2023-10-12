package com.alphabrik.msal;

import com.alphabrik.msal.model.Attachment;
import com.alphabrik.msal.model.AttachmentsResponse;
import com.alphabrik.msal.model.Message;
import com.alphabrik.msal.model.MessagesResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

public class MailServer {

    private static final Logger LOG = LoggerFactory.getLogger(MailServer.class);

    private static final HttpClient   HTTP_CLIENT = HttpClient.newBuilder()
                                                              .build();
    private static final ObjectMapper MAPPER      = new ObjectMapper();

    private enum MimeTypes {
        APPLICATION_JSON("application/json");

        public final String value;

        MimeTypes(String value) {
            this.value = value;
        }
    }

    private enum Headers {
        CONTENT_TYPE("Content-Type"),
        AUTHORIZATION("Authorization"),
        ACCEPT("Accept");

        public final String value;

        Headers(String value) {
            this.value = value;
        }
    }

    private final Configuration config;

    public MailServer(final Configuration config) {
        this.config = config;

        MAPPER.findAndRegisterModules();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public boolean connect() {
        try {
            final var app = ConfidentialClientApplication.builder(
                                                             config.getClientId(),
                                                             ClientCredentialFactory.createFromSecret(config.getClientSecret())
                                                         )
                                                         .authority(String.format("%s/%s", config.getAuthority(), config.getTenant()))
                                                         .build();

            final var clientCredentialParam = ClientCredentialParameters.builder(
                                                                            Collections.singleton("https://graph.microsoft.com/.default")
                                                                        )
                                                                        .build();
            final var result = app.acquireToken(clientCredentialParam).get();
            final var token  = result.accessToken();
            LOG.debug("Token received: {}", token);
            config.setAccessToken(token);
            return true;
        } catch (final Exception e) {
            LOG.warn("Can not connect to mail server", e);
            return false;
        }
    }

    public void toggleRead(final String id, final boolean read) throws Exception {
        final var request = HttpRequest.newBuilder()
                                       .method("PATCH", HttpRequest.BodyPublishers.ofString(String.format("{%s: %s}", "isRead", read)))
                                       .header(Headers.CONTENT_TYPE.value, MimeTypes.APPLICATION_JSON.value)
                                       .header(Headers.AUTHORIZATION.value, String.format("Bearer %s", config.getToken()))
                                       .header(Headers.ACCEPT.value, MimeTypes.APPLICATION_JSON.value)
                                       .uri(new URI(String.format("%s/users/%s/messages/%s", config.getBaseUrl(), config.getAccount(), id)))
                                       .build();
        final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Request failed: {} {}", response.statusCode(), response.body());
            }
            throw new RuntimeException("Can not toggle read! " + response.statusCode());
        }
        final var responseBody = response.body();
        LOG.debug("Received: {}", responseBody);
    }

    public void delete(final String id) throws Exception {
        final var request = HttpRequest.newBuilder()
                                       .DELETE()
                                       .header(Headers.AUTHORIZATION.value, String.format("Bearer %s", config.getToken()))
                                       .uri(new URI(String.format("%s/users/%s/messages/%s", config.getBaseUrl(), config.getAccount(), id)))
                                       .build();
        final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Request failed: {} {}", response.statusCode(), response.body());
            }
            throw new RuntimeException("Can not delete message " + id + "! " + response.statusCode());
        }
    }

    public List<Attachment> getAttachments(final String id) throws Exception {
        final var request = HttpRequest.newBuilder()
                                       .GET()
                                       .header(Headers.AUTHORIZATION.value, String.format("Bearer %s", config.getToken()))
                                       .header(Headers.ACCEPT.value, MimeTypes.APPLICATION_JSON.value)
                                       .uri(new URI(String.format(
                                           "%s/users/%s/messages/%s/attachments",
                                           config.getBaseUrl(),
                                           config.getAccount(),
                                           id
                                           // TODO $select but with contentBytes!
                                       )))
                                       .build();

        final var response     = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        final var responseBody = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            LOG.warn("Request failed: {} {}", response.statusCode(), responseBody);
            throw new RuntimeException("Can not get attachments! " + response.statusCode());
        }
        LOG.debug("Received: {}", responseBody);
        return MAPPER.readValue(responseBody, AttachmentsResponse.class).value();
    }

    public List<Message> getMessages(final boolean unreadOnly) throws Exception {
        final var request = HttpRequest.newBuilder()
                                       .GET()
                                       .header(Headers.AUTHORIZATION.value, String.format("Bearer %s", config.getToken()))
                                       .header(Headers.ACCEPT.value, MimeTypes.APPLICATION_JSON.value)
                                       .uri(new URI(String.format(
                                           "%s/users/%s/messages?%s&%s&%s",
                                           config.getBaseUrl(),
                                           config.getAccount(),
                                           "$select=subject,from,isRead,sentDateTime,receivedDateTime,hasAttachments",
                                           unreadOnly ? "$filter=isRead%20eq%20false" : "",
                                           "$orderby=receivedDateTime%20desc"
                                       )))
                                       .build();
        final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            throw new RuntimeException("Can not get messages! " + response.statusCode());
        }
        final var responseBody = response.body();
        LOG.debug("Received: {}", responseBody);
        return MAPPER.readValue(responseBody, MessagesResponse.class).value();
    }
}
