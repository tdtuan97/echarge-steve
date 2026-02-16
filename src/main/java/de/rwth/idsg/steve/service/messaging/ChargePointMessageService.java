package de.rwth.idsg.steve.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Publishes charge point lifecycle events to NATS JetStream stream
 * {@code CHARGE_POINT}.
 */
@Slf4j
@Service
public class ChargePointMessageService {
    private static final String CHARGE_POINT_SUBJECT_FORMAT = "charge_point.<tenant_type>.<tenant_code>.<charge_point_id>.<event>";
    private static final String TENANT_TYPE_KEY = "tenant_type";
    private static final String TENANT_CODE_KEY = "tenant_code";
    public static final String DEFAULT_TENANT_TYPE = "internal";
    public static final String DEFAULT_TENANT_CODE = "default";
    private final ObjectMapper objectMapper;
    private JetStream jetStream;
    private final NatsJetStreamService natsJetStreamService;
    @Value("${server.name:}")
    private String serverName;

    @Autowired
    public ChargePointMessageService(NatsJetStreamService natsJetStreamService) {
        this.natsJetStreamService = natsJetStreamService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JodaModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @PostConstruct
    public void init() {
        jetStream = natsJetStreamService.getJetStream();
    }

    /**
     * Publishes a charge point event to NATS.
     * Topic:
     * charge_point.&lt;tenant_type&gt;.&lt;tenant_code&gt;.&lt;charge_point_id&gt;.&lt;event&gt;
     *
     * @param params publish parameters (tenant, charge point id, event, payload)
     */
    public void publishChargePointEvent(ChargePointEventPublishParams params) {
        if (jetStream == null) {
            log.warn("⚠️ JetStream not ready, skip publish");
            return;
        }
        if (params == null) {
            return;
        }
        log.info("🔍 ------------ Server name: {}", serverName);
        String tenantTypeValue = Objects.toString(params.getTenantType(), "").trim().isEmpty() ? DEFAULT_TENANT_TYPE
                : params.getTenantType();
        String tenantCodeValue = Objects.toString(params.getTenantCode(), "").trim().isEmpty() ? DEFAULT_TENANT_CODE
                : params.getTenantCode();
        String chargePointIdValue = Objects.toString(params.getChargePointId(), "");
        String eventValue = Objects.toString(params.getEvent(), "");
        if (chargePointIdValue.isEmpty() || eventValue.isEmpty()) {
            return;
        }
        try {
            Instant publishedAt = Instant.now();
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("tenant_type", tenantTypeValue);
            messageMap.put("tenant_code", tenantCodeValue);
            messageMap.put("customer_code", params.getCustomerCode());
            messageMap.put("customer_id", params.getCustomerId());
            messageMap.put("charge_point_id", chargePointIdValue);
            messageMap.put("event", eventValue);
            messageMap.put("published_at", publishedAt.toString());
            messageMap.put("payload", params.getPayload());
            messageMap.put("server_name", serverName);
            publishToJetStream(new ChargePointNatsTemplate(messageMap, tenantTypeValue, tenantCodeValue,
                    chargePointIdValue, eventValue));
        } catch (Exception e) {
            log.error("❌ Failed to publish charge point event", e);
        }
    }

    public void publishSystemEvent(WebSocketSession session, String chargePointId, String chargePointEvent) {
        if (jetStream == null) {
            log.warn("⚠️ JetStream not ready, skip publish");
            return;
        }
        if (session == null) {
            return;
        }
        try {
            Instant publishedAt = Instant.now();
            Map<String, Object> chargePointMsg = buildChargePointMessage(session, chargePointId, chargePointEvent,
                    publishedAt.toString());
            String tenantType = Objects.toString(chargePointMsg.get(TENANT_TYPE_KEY), "");
            String tenantCode = Objects.toString(chargePointMsg.get(TENANT_CODE_KEY), "");
            publishToJetStream(new ChargePointNatsTemplate(chargePointMsg, tenantType, tenantCode, chargePointId,
                    chargePointEvent));
        } catch (Exception e) {
            log.error("❌ Failed to publish JetStream message", e);
        }
    }

    private Map<String, Object> buildChargePointMessage(WebSocketSession session, String chargePointId,
            String chargePointEvent, String publishedAt) {
        Map<String, String> sessionHeaders = WebSocketSessionMetadata.getSessionHeaders(session);
        String tenantType = WebSocketSessionMetadata.getTenantType(sessionHeaders);
        String tenantCode = WebSocketSessionMetadata.getTenantCode(sessionHeaders);
        String customerCode = WebSocketSessionMetadata.getCustomerCode(sessionHeaders);
        Long customerId = WebSocketSessionMetadata.getCustomerId(sessionHeaders);

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("tenant_type", tenantType);
        messageMap.put("tenant_code", tenantCode);
        messageMap.put("customer_code", customerCode);
        messageMap.put("customer_id", customerId);
        messageMap.put("charge_point_id", chargePointId);
        messageMap.put("event_type", chargePointEvent);
        messageMap.put("event_data", null);
        messageMap.put("published_at", publishedAt);
        messageMap.put("server_name", serverName);

        return messageMap;
    }

    private void publishToJetStream(ChargePointNatsTemplate payload) {
        try {
            Map<String, Object> envelopeMap = new HashMap<>();
            envelopeMap.put("uuid", UUID.randomUUID().toString());
            envelopeMap.put("timestamp", Instant.now().toEpochMilli());
            envelopeMap.put("message", payload.messageMap);
            byte[] payloadBytes = objectMapper.writeValueAsBytes(envelopeMap);
            String subject = NatsSubjectTemplate.buildSubject(
                    CHARGE_POINT_SUBJECT_FORMAT,
                    Map.of(
                            "<tenant_type>", Objects.toString(payload.tenantType, ""),
                            "<tenant_code>", Objects.toString(payload.tenantCode, ""),
                            "<charge_point_id>", Objects.toString(payload.chargePointId, ""),
                            "<event>", Objects.toString(payload.event, "")),
                    false);
            jetStream.publish(subject, payloadBytes);
        } catch (IOException | JetStreamApiException e) {
            log.error("❌ Failed to publish to JetStream", e);
        }
    }

    private static final class ChargePointNatsTemplate {
        final Map<String, Object> messageMap;
        final String tenantType;
        final String tenantCode;
        final String chargePointId;
        final String event;

        ChargePointNatsTemplate(Map<String, Object> messageMap, String tenantType, String tenantCode,
                String chargePointId, String event) {
            this.messageMap = messageMap;
            this.tenantType = tenantType;
            this.tenantCode = tenantCode;
            this.chargePointId = chargePointId;
            this.event = event;
        }
    }
}
