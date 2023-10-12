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
import java.util.stream.Collectors;

class MailExample {

    private static final Logger LOG = LoggerFactory.getLogger(MailExample.class);

    private static final HttpClient   HTTP_CLIENT    = HttpClient.newBuilder()
                                                                 .build();
    private static final ObjectMapper MAPPER         = new ObjectMapper();
    public static final  String       GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0";

    static {
        MAPPER.findAndRegisterModules();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void main(String[] args) throws Exception {
        final var config = new Configuration("/application.properties");

        final var app = ConfidentialClientApplication.builder(
                                                         config.getClientId(),
                                                         ClientCredentialFactory.createFromSecret(config.getClientSecret())
                                                     )
                                                     .authority(String.format("%s/%s", config.getAuthority(), config.getTenant()))
                                                     .build();

        final var clientCredentialParam = ClientCredentialParameters.builder(
                                                                        Collections.singleton("https://graph.microsoft.com/.default"))
                                                                    .build();
        final var result = app.acquireToken(clientCredentialParam).get();
        final var token  = result.accessToken();
        LOG.info("Token: {}", token);
        config.setAccessToken(token);

        final var messages = getMessages(config);
        if (LOG.isInfoEnabled()) {
            LOG.info(
                "Messages:\n{}",
                messages.stream()
                        .map(m -> String.format(
                                 "%s: %s - %s (%s) %s",
                                 m.id(),
                                 m.from(),
                                 m.subject(),
                                 m.receivedDateTime(),
                                 m.isRead() ? "" : "<*>"
                             )
                        )
                        .collect(Collectors.joining("\n\t"))
            );
        }
        toggleRead(config, messages.get(0));
    }

    private static void toggleRead(final Configuration config, final Message msg) throws Exception {
        final var request = HttpRequest.newBuilder()
                                       .method("PATCH", HttpRequest.BodyPublishers.ofString(String.format("{ %s: %s}", "isRead", !msg.isRead())))
                                       .header("Content-Type", "application/json")
                                       .header("Authorization", "Bearer " + config.getToken())
                                       .header("Accept", "application/json")
                                       .uri(new URI(String.format("%s/users/%s/messages/%s", GRAPH_BASE_URL, config.getAccount(), msg.id())))
                                       .build();
        final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Request failed: {} {}", response.statusCode(), response.body());
            }
            throw new RuntimeException("Can not get messages! " + response.statusCode());
        }
        final var responseBody = response.body();
        LOG.debug("Received: {}", responseBody);
    }

    private static List<Message> getMessages(final Configuration config) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
                                               .GET()
                                               .header("Authorization", "Bearer " + config.getToken())
                                               .header("Accept", "application/json")
                                               .uri(new URI(String.format("%s/users/%s/messages", GRAPH_BASE_URL, config.getAccount())))
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
