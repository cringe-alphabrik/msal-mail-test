package com.alphabrik.msal;

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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class AccessTokenAcquirer {

    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenAcquirer.class);

    private static final HttpClient   HTTP_CLIENT = HttpClient.newBuilder()
                                                              .build();
    private static final ObjectMapper MAPPER      = new ObjectMapper();

    static {
        MAPPER.findAndRegisterModules();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void main(String[] args) throws Exception {
        final var props = new Properties();
        try {
            props.load(AccessTokenAcquirer.class.getResourceAsStream("/application.properties"));
        } catch (final Exception e) {
            LOG.error("Can not load properties!", e);
            System.exit(-1);
        }

        final var app = ConfidentialClientApplication.builder(
                                                         getOrThrow(props, "clientId"),
                                                         ClientCredentialFactory.createFromSecret(getOrThrow(props, "clientSecret"))
                                                     )
                                                     .authority("https://login.microsoftonline.com/" + getOrThrow(props, "tenant"))
                                                     .build();

        final var clientCredentialParam = ClientCredentialParameters.builder(
                                                                        Collections.singleton("https://graph.microsoft.com/.default"))
                                                                    .build();
        final var result         = app.acquireToken(clientCredentialParam).get();
        final var token          = result.accessToken();
        final var tokenExpiresOn = result.expiresOnDate();
        LOG.info("Token: {}", token);
        props.setProperty("access_token", token);
        props.setProperty("access_token_expires_on", tokenExpiresOn.toInstant().toString());

        // TODO get messages for account
        final var messages = getMessages(props);
        LOG.info(
            "Messages:\n{}",
            messages.stream()
                    .map(m -> String.format("%s: %s - %s (%s)", m.internetMessageId, m.from, m.subject, m.receivedDateTime))
                    .collect(Collectors.joining("\n\t"))
        )
        ;
    }

    private static List<Message> getMessages(final Properties properties) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
                                               .GET()
                                               .header("Authorization", "Bearer " + getOrThrow(properties, "access_token"))
                                               .header("Accept", "application/json")
                                               .uri(new URI("https://graph.microsoft.com/v1.0" + "/users/" + getOrThrow(properties, "account") + "/messages"))
                                               .build();
        final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Can not get messages! " + response.statusCode());
        }
        final var body = response.body();
        LOG.debug("Received: {}", body);
        return MAPPER.readValue(body, MessagesResponse.class).value();
    }

    private static String getOrThrow(final Properties props, final String property, final Supplier<Exception> exceptionSupplier) throws Exception {
        if (props.containsKey(property)) {
            return props.getProperty(property);
        } else {
            throw exceptionSupplier.get();
        }
    }

    private static String getOrThrow(final Properties props, final String property) throws Exception {
        return getOrThrow(props, property, () -> new IllegalArgumentException(property + " is not set!"));
    }

    record MessagesResponse(List<Message> value) {

    }

    record Message(
        String id,
        String internetMessageId,
        From from,
        String subject,
        String bodyPreview,
        Instant receivedDateTime
    ) {

    }

    record From(
        EmailAddress emailAddress
    ) {

        @Override
        public String toString() {
            return String.format("%s <%s>", emailAddress.name, emailAddress.address);
        }
    }

    record EmailAddress(
        String name,
        String address
    ) {

    }
}
