package com.alphabrik.msal;

import com.alphabrik.msal.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

class MailExample {

    private static final Logger LOG = LoggerFactory.getLogger(MailExample.class);

    private static Function<Message, String> formatMessage() {
        return m -> String.format(
            "%s: %s - %s (%s) %s",
            m.id(),
            m.from(),
            m.subject(),
            m.receivedDateTime(),
            m.isRead() ? "" : "<*>"
        );
    }

    public static void main(String[] args) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("args: {}", Arrays.toString(args));
        }

        final var config = new Configuration(MailExample.class.getResourceAsStream("/application.properties"));

        final var server = new MailServer(config);
        if (!server.connect()) {
            LOG.error("Can not connect to mail server!");
            System.exit(-1);
        }

        if (args.length == 0) {
            LOG.info("No command, exiting.");
            System.exit(0);
        }

        switch (args[0]) {
            case "getMessages" -> {
                if (args.length != 2) {
                    LOG.warn("Usage: getMessages <unreadOnly>");
                    break;
                }
                final var messages = server.getMessages(Boolean.parseBoolean(args[1]));
                if (!messages.isEmpty()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Messages:\n\t{}", messages.stream()
                                                          .map(formatMessage())
                                                          .collect(Collectors.joining("\n\t"))
                        );
                    }
                } else {
                    LOG.info("No messages found!");
                }
            }
            case "toggleRead" -> {
                if (args.length != 3) {
                    LOG.warn("Usage: toggleRead <id> <true|false>");
                    break;
                }
                server.toggleRead(args[1], Boolean.parseBoolean(args[2]));
            }
            case "delete" -> {
                if (args.length != 2) {
                    LOG.warn("Usage: delete <id>");
                    break;
                }
                server.delete(args[1]);
            }
            default -> LOG.info("Unknown command!");
        }
    }
}
