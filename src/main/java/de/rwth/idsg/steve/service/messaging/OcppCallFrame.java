package de.rwth.idsg.steve.service.messaging;

import java.util.Map;

/**
 * OCPP JSON frame decoded from websocket messages.
 */
public final class OcppCallFrame {
    private final int messageTypeId;
    private final String messageType;
    private final String messageId;
    private final String action;
    private final Map<String, Object> payload;

    public OcppCallFrame(
            int messageTypeId,
            String messageType,
            String messageId,
            String action,
            Map<String, Object> payload
    ) {
        this.messageTypeId = messageTypeId;
        this.messageType = messageType;
        this.messageId = messageId;
        this.action = action;
        this.payload = payload;
    }

    public int getMessageTypeId() {
        return messageTypeId;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}

