/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2026 SteVe Community Team
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.web.dto;

import de.rwth.idsg.steve.ocpp.OcppVersion;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Map;

/**
 * OCPP JSON (WebSocket) connection status with tenant and metadata from handshake headers.
 * Clone of ocpp-steve OcppJsonStatus with tenantType, tenantCode and metadata added.
 */
@Getter
@Builder
@ToString
public final class OcppJsonStatus {
    private final int chargeBoxPk;
    private final String chargeBoxId;
    private final String connectedSince;
    private final String connectionDuration;
    private final OcppVersion version;
    private final DateTime connectedSinceDT;
    /** Tenant type from WebSocket handshake headers (e.g. X-Tenant-Type). Default "internal" when absent. */
    @Builder.Default private final String tenantType = "internal";
    /** Tenant code from WebSocket handshake headers (e.g. X-Tenant-Code). Default "default" when absent. */
    @Builder.Default private final String tenantCode = "default";
    /** Customer code from WebSocket handshake headers (e.g. X-Customer-Code). Null when absent. */
    private final String customerCode;
    /** Customer id from WebSocket handshake headers (e.g. X-Customer-Id). Null when absent. */
    private final Long customerId;
    /** Session metadata: ip, session_id, forward (path, protocol, port, host). */
    @Builder.Default private final Map<String, Object> metadata = Collections.emptyMap();
}
