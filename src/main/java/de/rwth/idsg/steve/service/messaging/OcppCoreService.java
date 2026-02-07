package de.rwth.idsg.steve.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Publishes OCPP core events to NATS stream {@code OCPP_CORE}.
 */
@Slf4j
@Service
public class OcppCoreService {
    private static final String OCPP_CORE_SUBJECT_FORMAT = "ocpp.<version>.<tenant_type>.<tenant_code>.<direction>.<charge_point_id>.<action>";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private JetStream jetStream;
    private final NatsJetStreamService natsJetStreamService;

    @Autowired
    public OcppCoreService(NatsJetStreamService natsJetStreamService) {
        this.natsJetStreamService = natsJetStreamService;
    }

    @PostConstruct
    public void init() {
        jetStream = natsJetStreamService.getJetStream();
    }

    /**
     * Publish OCPP core message to NATS JetStream stream {@code OCPP_CORE}.
     * 
     * @param session
     * @param chargePointId
     * @param direction
     * @param rawMessage
     */
    public void publish(WebSocketSession session, String chargePointId, OcppDirection ocppDirection, String rawMessage) {
        if (jetStream == null) {
            log.warn("⚠️ JetStream not ready, skip publish");
            return;
        }
        if (session == null) {
            log.warn("⚠️ WebSocketSession is null, skip publish");
            return;
        }
        String chargePointIdValue = Objects.toString(chargePointId, "");
        String directionValue = ocppDirection.getValue();
        String rawMessageValue = Objects.toString(rawMessage, "");
        if (chargePointIdValue.isEmpty() || directionValue.isEmpty() || rawMessageValue.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> ocppCoreMessage = buildOcppCoreMessage(session, chargePointIdValue, directionValue,
                    rawMessageValue);
            if (ocppCoreMessage == null) {
                log.error("[chargePointId={}] fail to build OCPP core message", chargePointId);
                return;
            }
            String ocppVersionValue = Objects.toString(ocppCoreMessage.get("ocpp_version"), "");
            String tenantTypeValue = Objects.toString(ocppCoreMessage.get("tenant_type"), "");
            String tenantCodeValue = Objects.toString(ocppCoreMessage.get("tenant_code"), "");
            String actionValue = Objects.toString(ocppCoreMessage.get("action"), "");
            if (ocppVersionValue.isEmpty() || tenantTypeValue.isEmpty() || tenantCodeValue.isEmpty()
                    || actionValue.isEmpty()) {
                return;
            }
            byte[] ocppCorePayloadBytes = objectMapper.writeValueAsBytes(ocppCoreMessage);
            String streamSubject = NatsSubjectTemplate.buildSubject(
                    OCPP_CORE_SUBJECT_FORMAT,
                    Map.of(
                            "<version>", ocppVersionValue,
                            "<tenant_type>", tenantTypeValue,
                            "<tenant_code>", tenantCodeValue,
                            "<direction>", directionValue,
                            "<charge_point_id>", chargePointIdValue,
                            "<action>", actionValue),
                    true);
            jetStream.publish(streamSubject, ocppCorePayloadBytes);
        } catch (Exception e) {
            log.error("❌ Failed to publish OCPP core message", e);
        }
    }

    /**
     * Build OCPP core message
     * @param session
     * @param chargePointId
     * @param direction
     * @param rawMessage
     * @return
     */
    private Map<String, Object> buildOcppCoreMessage(WebSocketSession session, String chargePointId, String direction,
            String rawMessage) {
        Map<String, String> sessionHeaders = WebSocketSessionMetadata.getSessionHeaders(session);
        String tenantType = WebSocketSessionMetadata.getTenantType(sessionHeaders);
        String tenantCode = WebSocketSessionMetadata.getTenantCode(sessionHeaders);
        String ocppVersion = WebSocketSessionMetadata.getOcppVersion(session, sessionHeaders);
        OcppCallFrame frame = parseOcppCall(rawMessage);
        if (frame == null) {
            log.error("[chargePointId={}] fail to parse message to OcppCallFrame", chargePointId);
            return null;
        }
        String normalizedOcppVersion = normalizeOcppVersion(ocppVersion);
        Map<String, Object> ocppCoreMessage = new LinkedHashMap<>();
        ocppCoreMessage.put("message_type_id", frame.getMessageTypeId());
        ocppCoreMessage.put("message_type", frame.getMessageType());
        ocppCoreMessage.put("message_id", frame.getMessageId());
        ocppCoreMessage.put("ocpp_version", normalizedOcppVersion);
        ocppCoreMessage.put("direction", direction);
        ocppCoreMessage.put("tenant_type", Objects.toString(tenantType, ""));
        ocppCoreMessage.put("tenant_code", Objects.toString(tenantCode, ""));
        ocppCoreMessage.put("charge_point_id", chargePointId);
        ocppCoreMessage.put("action", frame.getAction());
        ocppCoreMessage.put("payload", frame.getPayload());
        ocppCoreMessage.put("published_at", Instant.now().toString());
        ocppCoreMessage.put("metadata", WebSocketSessionMetadata.buildMetadata(session.getId(), sessionHeaders));
        return ocppCoreMessage;
    }

    @SuppressWarnings("unchecked")
    private OcppCallFrame parseOcppCall(String rawMessage) {
        try {
            List<Object> frame = objectMapper.readValue(rawMessage, List.class);
            if (frame == null || frame.size() < 2) {
                return null;
            }
            Integer messageTypeIdValue = (Integer) frame.get(0);
            if (messageTypeIdValue == null) {
                return null;
            }
            int messageTypeId = messageTypeIdValue;
            String messageId = frame.get(1) instanceof String ? (String) frame.get(1) : "";
            String messageType = switch (messageTypeId) {
                case 2 -> "call";
                case 3 -> "callresult";
                case 4 -> "callerror";
                default -> "unknown";
            };
            return switch (messageTypeId) {
                case 2 -> parseCallFrame(messageTypeId, messageType, messageId, frame);
                case 3 -> parseCallResultFrame(messageTypeId, messageType, messageId, frame);
                case 4 -> parseCallErrorFrame(messageTypeId, messageType, messageId, frame);
                default -> new OcppCallFrame(messageTypeId, messageType, messageId, "unknown", Collections.emptyMap());
            };
        } catch (Exception e) {
            log.debug("Failed to parse OCPP frame", e);
            return null;
        }
    }

    private OcppCallFrame parseCallFrame(int messageTypeId, String messageType, String messageId, List<Object> frame) {
        if (frame.size() < 4) {
            throw new IllegalArgumentException("Invalid CALL frame size");
        }
        if (!(frame.get(2) instanceof String)) {
            throw new IllegalArgumentException("Invalid CALL frame action");
        }
        String action = (String) frame.get(2);
        Object payloadValue = frame.get(3);
        if (payloadValue == null) {
            return new OcppCallFrame(messageTypeId, messageType, messageId, action, Collections.emptyMap());
        }
        if (!(payloadValue instanceof Map)) {
            throw new IllegalArgumentException("Invalid CALL frame payload");
        }
        Map<String, Object> payload = (Map<String, Object>) payloadValue;
        return new OcppCallFrame(messageTypeId, messageType, messageId, action, payload);
    }

    private OcppCallFrame parseCallResultFrame(int messageTypeId, String messageType, String messageId,
            List<Object> frame) {
        if (frame.size() < 3) {
            throw new IllegalArgumentException("Invalid CALLRESULT frame size");
        }
        String action = "callresult";
        Object payloadValue = frame.get(2);
        if (payloadValue == null) {
            return new OcppCallFrame(messageTypeId, messageType, messageId, action, Collections.emptyMap());
        }
        if (!(payloadValue instanceof Map)) {
            throw new IllegalArgumentException("Invalid CALLRESULT frame payload");
        }
        Map<String, Object> payload = (Map<String, Object>) payloadValue;
        return new OcppCallFrame(messageTypeId, messageType, messageId, action, payload);
    }

    private OcppCallFrame parseCallErrorFrame(int messageTypeId, String messageType, String messageId,
            List<Object> frame) {
        if (frame.size() < 5) {
            throw new IllegalArgumentException("Invalid CALLERROR frame size");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("errorCode", frame.get(2));
        payload.put("errorDescription", frame.get(3));
        payload.put("errorDetails", frame.get(4));
        return new OcppCallFrame(messageTypeId, messageType, messageId, "callerror", payload);
    }

    private String normalizeOcppVersion(String rawOcppVersion) {
        String sanitized = Objects.toString(rawOcppVersion, "").trim();
        if (sanitized.isEmpty()) {
            return "";
        }
        String digitsOnly = sanitized.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return "";
        }
        return "v" + digitsOnly;
    }
}
