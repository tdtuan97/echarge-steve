package de.rwth.idsg.steve.service.messaging;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Holds a singleton NATS connection and exposes JetStream contexts.
 * Config is read from Spring application properties: evc.nats.url, evc.nats.username, evc.nats.password.
 */
@Slf4j
@Service
public class NatsJetStreamService {

    @Value("${evc.nats.url:}")
    private String natsServerUrl;

    @Value("${evc.nats.username:}")
    private String natsUsername;

    @Value("${evc.nats.password:}")
    private String natsPassword;

    private Connection natsConnection;
    private JetStream jetStream;
    private JetStreamManagement jetStreamManagement;

    @PostConstruct
    public void init() {
        if (natsServerUrl == null || natsServerUrl.isBlank()) {
            log.warn("NATS URL not configured (evc.nats.url), skipping NATS connection");
            return;
        }
        try {
            String natsServerUrlWithCredentials = buildNatsServerUrlWithCredentials(natsServerUrl, natsUsername, natsPassword);
            natsConnection = Nats.connect(natsServerUrlWithCredentials);
            jetStream = natsConnection.jetStream();
            jetStreamManagement = natsConnection.jetStreamManagement();
            log.info("✅ Connected to NATS broker at {}", natsServerUrl);
        } catch (Exception e) {
            log.error("❌ Failed to connect to NATS broker", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (natsConnection != null) {
                natsConnection.close();
                log.info("🔌 NATS connection closed");
            }
        } catch (Exception e) {
            log.error("Error closing NATS connection", e);
        }
    }

    public JetStream getJetStream() {
        return jetStream;
    }

    public JetStreamManagement getJetStreamManagement() {
        return jetStreamManagement;
    }

    private String buildNatsServerUrlWithCredentials(String natsServerUrl, String natsUsername, String natsPassword) {
        if (natsUsername == null || natsUsername.isBlank()) {
            return natsServerUrl;
        }
        if (natsPassword == null || natsPassword.isBlank()) {
            return natsServerUrl;
        }
        return Arrays.stream(natsServerUrl.split(","))
                .map(String::trim)
                .map(serverUrl -> addCredentialsToServerUrl(serverUrl, natsUsername, natsPassword))
                .collect(Collectors.joining(","));
    }

    private String addCredentialsToServerUrl(String serverUrl, String natsUsername, String natsPassword) {
        if (serverUrl.contains("@")) {
            return serverUrl;
        }
        int schemeSeparatorIndex = serverUrl.indexOf("://");
        if (schemeSeparatorIndex < 0) {
            return serverUrl;
        }
        String scheme = serverUrl.substring(0, schemeSeparatorIndex + 3);
        String remainder = serverUrl.substring(schemeSeparatorIndex + 3);
        return scheme + natsUsername + ":" + natsPassword + "@" + remainder;
    }
}

