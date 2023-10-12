package com.alphabrik.msal;

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

    private static final HttpClient   HTTP_CLIENT      = HttpClient.newBuilder()
                                                                   .build();
    private static final ObjectMapper MAPPER           = new ObjectMapper();
    public static final  String       APPLICATION_JSON = "application/json";

    private final Configuration                 config;
    private       ConfidentialClientApplication app;

    public MailServer(final Configuration config) {
        this.config = config;

        MAPPER.findAndRegisterModules();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public boolean connect() {
        try {
            app = ConfidentialClientApplication.builder(
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
            LOG.info("Token: {}", token);
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
                                       .header("Content-Type", APPLICATION_JSON)
                                       .header("Authorization", String.format("Bearer %s", config.getToken()))
                                       .header("Accept", APPLICATION_JSON)
                                       .uri(new URI(String.format("%s/users/%s/messages/%s", config.getBaseUrl(), config.getAccount(), id)))
                                       .build();
        final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Request failed: {} {}", response.statusCode(), response.body());
            }
            throw new RuntimeException("Can not get messages! " + response.statusCode());
        }
        final var responseBody = response.body();
        LOG.debug("Received: {}", responseBody);
    }

    public List<Message> getMessages() throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
                                               .GET()
                                               .header("Authorization", "Bearer " + config.getToken())
                                               .header("Accept", APPLICATION_JSON)
                                               .uri(new URI(String.format("%s/users/%s/messages", config.getBaseUrl(), config.getAccount())))
                                               .build();
        final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Can not get messages! " + response.statusCode());
        }
        final var responseBody = response.body();
        LOG.debug("Received: {}", responseBody);
        return MAPPER.readValue(responseBody, MessagesResponse.class).value();
    }
}
