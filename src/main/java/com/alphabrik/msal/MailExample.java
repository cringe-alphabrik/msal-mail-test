package com.alphabrik.msal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

class MailExample {

    private static final Logger LOG = LoggerFactory.getLogger(MailExample.class);

    public static void main(String[] args) throws Exception {
        final var config = new Configuration(MailExample.class.getResourceAsStream("/application.properties"));

        final var server = new MailServer(config);
        if (!server.connect()) {
            LOG.error("Can not connect to mail server!");
            System.exit(-1);
        }

        final var messages = server.getMessages();
        if (!messages.isEmpty()) {
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
            final var msg = messages.get(0);
            server.toggleRead(msg.id(), !msg.isRead());
        } else {
            LOG.info("No messages found!");
        }
    }
}
