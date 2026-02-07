package de.rwth.idsg.steve.service.messaging;

import java.util.Map;
import java.util.Objects;

/**
 * Helper for building NATS subjects from a string template with placeholders like {@code <tenant_id>}.
 */
public final class NatsSubjectTemplate {
    private static final String UNKNOWN_TOKEN = "unknown";

    private NatsSubjectTemplate() {
    }

    public static String buildSubject(String template, Map<String, String> placeholders, boolean isSanitized) {
        String subject = Objects.toString(template, "");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();
            String rendered = isSanitized ? sanitizeToken(value) : Objects.toString(value, "").trim();
            subject = subject.replace(placeholder, rendered);
        }
        return subject;
    }

    private static String sanitizeToken(String raw) {
        String value = Objects.toString(raw, "").trim();
        if (value.isEmpty()) {
            return UNKNOWN_TOKEN;
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9_-]", "");
        if (sanitized.isEmpty()) {
            return UNKNOWN_TOKEN;
        }
        return sanitized;
    }
}

