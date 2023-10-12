package com.alphabrik.msal;

import com.alphabrik.msal.model.Attachment;
import com.alphabrik.msal.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;

class MailExample {

    private static final Logger LOG = LoggerFactory.getLogger(MailExample.class);

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

        final var scanner = new Scanner(System.in);
        switch (args[0]) {
            case "getAttachments" -> {
                String id;
                if (args.length != 2) {
                    LOG.warn("Usage: getAttachments <id>");
                    System.out.println("Please enter the message id to get attachments for:");
                    id = scanner.nextLine();
                } else {
                    id = args[1];
                }
                final var attachments = server.getAttachments(id);
                if (!attachments.isEmpty()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Attachments:\n\t{}", attachments.stream()
                                                                  .map(Attachment::toString)
                                                                  .collect(Collectors.joining("\n\t"))
                        );
                    }
                } else {
                    LOG.info("No attachments found!");
                }
            }
            case "getMessages" -> {
                boolean read;
                if (args.length != 2) {
                    LOG.warn("Usage: getMessages <unreadOnly>");
                    System.out.println("Please enter the new read status (true|false):");
                    read = scanner.nextBoolean();
                } else {
                    read = Boolean.parseBoolean(args[1]);
                }
                final var messages = server.getMessages(read);
                if (!messages.isEmpty()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Messages:\n\t{}", messages.stream()
                                                            .map(Message::toString)
                                                            .collect(Collectors.joining("\n\t"))
                        );
                    }
                } else {
                    LOG.info("No messages found!");
                }
            }
            case "toggleRead" -> {
                String  id;
                boolean read;
                if (args.length != 3) {
                    LOG.warn("Usage: toggleRead <id> <true|false>");
                    System.out.println("Please enter the message id to toggle read status:");
                    id = scanner.nextLine();
                    System.out.println("Please enter the new read status (true|false):");
                    read = scanner.nextBoolean();
                } else {
                    id = args[1];
                    read = Boolean.parseBoolean(args[2]);
                }
                server.toggleRead(id, read);
            }
            case "delete" -> {
                if (args.length != 2) {
                    LOG.warn("Usage: delete <id>");
                    System.out.println("Please enter the message id to delete:");
                    final var id      = scanner.nextLine();
                    server.delete(id);
                } else {
                    server.delete(args[1]);
                }
            }
            default -> LOG.info("Unknown command! Available commands: getMessages, toggleRead, delete");
        }
    }
}
