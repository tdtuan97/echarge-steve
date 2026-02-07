package de.rwth.idsg.steve.service.messaging;

import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper for ensuring JetStream streams exist.
 */
@Slf4j
public final class JetStreamStreamInitializer {
    private static final long STREAM_MAX_AGE_DAYS = 7L;

    private JetStreamStreamInitializer() {
    }

    public static void ensureStreamExists(JetStreamManagement jetStreamManagement, String streamName, String subjectPrefix) {
        if (jetStreamManagement == null) {
            log.warn("⚠️ JetStreamManagement not ready, skip stream initialization");
            return;
        }
        try {
            StreamInfo streamInfo = jetStreamManagement.getStreamInfo(streamName);
            log.info("✅ JetStream stream '{}' already exists", streamName);
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() != 404) {
                log.error("❌ Failed to check JetStream stream '{}'", streamName, e);
                return;
            }
            try {
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name(streamName)
                        .subjects(subjectPrefix + ".>")
                        .storageType(io.nats.client.api.StorageType.File)
                        .retentionPolicy(io.nats.client.api.RetentionPolicy.Limits)
                        .maxAge(java.time.Duration.ofDays(STREAM_MAX_AGE_DAYS))
                        .build();
                jetStreamManagement.addStream(streamConfig);
                log.info("✅ Created JetStream stream '{}' [{}]", streamName, subjectPrefix + ".>");
            } catch (Exception ex) {
                log.error("❌ Failed to create JetStream stream '{}'", streamName, ex);
            }
        } catch (Exception e) {
            log.error("❌ Error initializing JetStream stream '{}'", streamName, e);
        }
    }
}

