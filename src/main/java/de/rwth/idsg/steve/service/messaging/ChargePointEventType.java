package de.rwth.idsg.steve.service.messaging;

/**
 * Charge point event types for NATS publish (subject suffix).
 */
public enum ChargePointEventType {
    CONNECTED("connected"),
    DISCONNECTED("disconnected"),
    FAILED("failed"),
    BOOTED("booted"),
    HEARTBEAT("heartbeat"),
    CONNECTOR_STATUS_CHANGED("connector.status.changed"),
    TRANSACTION_STARTED("transaction.started"),
    TRANSACTION_STOPPED("transaction.stopped"),
    METER_UPDATED("meter.updated"),
    DATA_TRANSFER("data.transfer"),
    AUTHORIZE("authorize");

    private final String value;

    ChargePointEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
