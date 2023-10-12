package com.alphabrik.msal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.function.Supplier;

class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    private final Properties props = new Properties();

    public Configuration(final String file) {
        try {
            props.load(MailExample.class.getResourceAsStream(file));
        } catch (final Exception e) {
            LOG.error("Can not load properties from file " + file, e);
            System.exit(-1);
        }
    }

    private String getOrThrow(final String property, final Supplier<Exception> exceptionSupplier) throws Exception {
        if (props.containsKey(property)) {
            return props.getProperty(property);
        } else {
            throw exceptionSupplier.get();
        }
    }

    public String getOrThrow(final String property) throws Exception {
        return getOrThrow(property, () -> new IllegalArgumentException(property + " is not set!"));
    }

    public String getAccount() throws Exception {
        return getOrThrow("account");
    }

    public String getToken() throws Exception {
        return getOrThrow("access_token");
    }

    public String getAuthority() throws Exception {
        return getOrThrow("authority");
    }

    public String getTenant() throws Exception {
        return getOrThrow("tenant");
    }

    public String getClientSecret() throws Exception {
        return getOrThrow("clientSecret");
    }

    public String getClientId() throws Exception {
        return getOrThrow("clientId");
    }

    public void setAccessToken(final String token) {
        props.setProperty("access_token", token);
    }
}
