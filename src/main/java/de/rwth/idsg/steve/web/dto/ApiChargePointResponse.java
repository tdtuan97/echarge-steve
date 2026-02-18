/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2025 SteVe Community Team
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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API response DTO for charge point with basic fields aligned to ChargePointForm.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Charge point response with basic fields")
public class ApiChargePointResponse {

    @Schema(description = "Internal database primary key")
    private Integer chargeBoxPk;

    @Schema(description = "ChargeBox ID (OCPP identity)")
    private String chargeBoxId;

    @Schema(description = "Registration status")
    private String registrationStatus;

    @Schema(description = "Whether to insert connector status after transaction message")
    private Boolean insertConnectorStatusAfterTransactionMsg;

    @Schema(description = "Address")
    private Address address;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Note")
    private String note;

    @Schema(description = "Admin address URL")
    private String adminAddress;

    @Schema(description = "OCPP security profile value")
    private String securityProfile;

    @Schema(description = "Whether auth password is set")
    private Boolean hasAuthPassword;

    @Schema(description = "OCPP protocol (e.g. ocpp1.6J)")
    private String ocppProtocol;

    @Schema(description = "Last heartbeat timestamp (humanized)")
    private String lastHeartbeatTimestamp;

    @Schema(description = "Charge point vendor")
    private String chargePointVendor;

    @Schema(description = "Charge point model")
    private String chargePointModel;
}
