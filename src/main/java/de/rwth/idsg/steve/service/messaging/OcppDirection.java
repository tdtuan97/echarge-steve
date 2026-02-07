package de.rwth.idsg.steve.service.messaging;

import java.util.Objects;

/**
 * Direction of an OCPP message on the WebSocket connection.
 */
public enum OcppDirection {
    IN("in"),
    OUT("out");

    private final String value;

    OcppDirection(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OcppDirection fromValue(String direction) {
        String value = Objects.toString(direction, "").trim().toLowerCase();
        if (IN.value.equals(value)) {
            return IN;
        }
        if (OUT.value.equals(value)) {
            return OUT;
        }
        return null;
    }
}

