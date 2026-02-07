package de.rwth.idsg.steve.service.messaging;

import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Helpers for extracting metadata from a {@link WebSocketSession}.
 */
public final class WebSocketSessionMetadata {
    private WebSocketSessionMetadata() {
    }

    public static Map<String, String> getSessionHeaders(WebSocketSession session) {
        Map<String, String> headers = new HashMap<>();
        session.getHandshakeHeaders().forEach((key, values) -> {
            String value = values == null || values.isEmpty() ? "" : String.valueOf(values.get(0));
            //if ("authorization".equalsIgnoreCase(key) || "cookie".equalsIgnoreCase(key)) {
            //    headers.put(key, "[redacted]");
            //    return;
            //}
            headers.put(key, value);
        });
        return headers;
    }

    public static String getTenantCode(Map<String, String> sessionHeaders) {
        String tenantCode = getHeaderValueIgnoreCase(sessionHeaders, "X-Tenant-Code");
        if (!tenantCode.isEmpty()) {
            return tenantCode;
        }
        tenantCode = getHeaderValueIgnoreCase(sessionHeaders, "Tenant-Code");
        if (!tenantCode.isEmpty()) {
            return tenantCode;
        }
        tenantCode = getHeaderValueIgnoreCase(sessionHeaders, "TenantCode");
        if (!tenantCode.isEmpty()) {
            return tenantCode;
        }
        return "default";
    }

    public static String getTenantType(Map<String, String> sessionHeaders) {
        String tenantType = getHeaderValueIgnoreCase(sessionHeaders, "X-Tenant-Type");
        if (!tenantType.isEmpty()) {
            return tenantType;
        }
        tenantType = getHeaderValueIgnoreCase(sessionHeaders, "Tenant-Type");
        if (!tenantType.isEmpty()) {
            return tenantType;
        }
        tenantType = getHeaderValueIgnoreCase(sessionHeaders, "TenantType");
        if (!tenantType.isEmpty()) {
            return tenantType;
        }
        return "internal";
    }

    /**
     * Extracts customer code from session headers.
     * Checks X-Customer-Code, Customer-Code, CustomerCode. Returns null if not present.
     */
    public static String getCustomerCode(Map<String, String> sessionHeaders) {
        String value = getHeaderValueIgnoreCase(sessionHeaders, "X-Customer-Code");
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = getHeaderValueIgnoreCase(sessionHeaders, "Customer-Code");
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = getHeaderValueIgnoreCase(sessionHeaders, "CustomerCode");
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return null;
    }

    /**
     * Extracts customer id from session headers.
     * Checks X-Customer-Id, Customer-Id, CustomerId. Returns null if not present or invalid.
     */
    public static Long getCustomerId(Map<String, String> sessionHeaders) {
        String value = getHeaderValueIgnoreCase(sessionHeaders, "X-Customer-Id");
        if (value == null || value.isEmpty()) {
            value = getHeaderValueIgnoreCase(sessionHeaders, "Customer-Id");
        }
        if (value == null || value.isEmpty()) {
            value = getHeaderValueIgnoreCase(sessionHeaders, "CustomerId");
        }
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException err) {
            return null;
        }
    }

    public static String getOcppVersion(WebSocketSession session, Map<String, String> sessionHeaders) {
        String acceptedProtocol = session.getAcceptedProtocol();
        if (acceptedProtocol != null && !acceptedProtocol.isEmpty()) {
            return acceptedProtocol;
        }
        return getHeaderValueIgnoreCase(sessionHeaders, "Sec-WebSocket-Protocol");
    }

    public static Map<String, Object> buildMetadata(String sessionId, Map<String, String> sessionHeaders) {
        Map<String, String> safeSessionHeaders = sessionHeaders == null ? Collections.emptyMap() : sessionHeaders;
        String ip = getHeaderValueIgnoreCase(safeSessionHeaders, "X-Real-IP");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ip", ip);
        metadata.put("session_id", Objects.toString(sessionId, ""));
        Map<String, Object> forward = buildForwardMetadata(safeSessionHeaders);
        if (!forward.isEmpty()) {
            metadata.put("forward", forward);
        }
        return metadata;
    }

    private static Map<String, Object> buildForwardMetadata(Map<String, String> safeSessionHeaders) {
        String forwardedHost = getHeaderValueIgnoreCase(safeSessionHeaders, "X-Forwarded-Host");
        String forwardedProto = getHeaderValueIgnoreCase(safeSessionHeaders, "X-Forwarded-Proto");
        String forwardedPort = getHeaderValueIgnoreCase(safeSessionHeaders, "X-Forwarded-Port");
        String forwardedPath = getHeaderValueIgnoreCase(safeSessionHeaders, "X-Forwarded-Path");
        Map<String, Object> forward = new HashMap<>();
        if (!forwardedHost.isEmpty()) {
            forward.put("host", forwardedHost);
        }
        Integer numericPort = parsePort(forwardedPort);
        if (numericPort != null) {
            forward.put("port", numericPort);
        }
        if (!forwardedProto.isEmpty()) {
            forward.put("protocol", forwardedProto);
        }
        if (!forwardedPath.isEmpty()) {
            forward.put("path", forwardedPath);
        }
        return forward;
    }

    private static Integer parsePort(String port) {
        if (port == null || port.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException err) {
            return null;
        }
    }

    public static String getHeaderValueIgnoreCase(Map<String, String> sessionHeaders, String headerName) {
        return sessionHeaders.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName))
                .map(entry -> Objects.toString(entry.getValue(), ""))
                .findFirst()
                .orElse("");
    }
}

