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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.joda.time.DateTime;

/**
 * API response DTO for transaction detail by transaction PK.
 * Matches the UI columns: Transaction ID, ChargeBox ID, Connector ID, OCPP ID Tag, User ID,
 * Start Date/Time, Start Value, Stop Date/Time, Stop Value.
 */
@Schema(description = "Transaction detail matching UI columns")
@Getter
@Builder
public class ApiTransactionDetailResponse {

    @JsonProperty("transaction_pk")
    @Schema(description = "Transaction ID")
    private final Integer transactionPk;

    @JsonProperty("charge_box_id")
    @Schema(description = "ChargeBox ID")
    private final String chargeBoxId;

    @JsonProperty("connector_id")
    @Schema(description = "Connector ID")
    private final Integer connectorId;

    @JsonProperty("id_tag")
    @Schema(description = "OCPP ID Tag")
    private final String idTag;

    @JsonProperty("user_id")
    @Schema(description = "User ID", nullable = true)
    private final Integer userId;

    @JsonProperty("start_timestamp")
    @Schema(description = "Start Date/Time")
    private final DateTime startTimestamp;

    @JsonProperty("start_value")
    @Schema(description = "Start Value")
    private final String startValue;

    @JsonProperty("stop_timestamp")
    @Schema(description = "Stop Date/Time", nullable = true)
    private final DateTime stopTimestamp;

    @JsonProperty("stop_value")
    @Schema(description = "Stop Value", nullable = true)
    private final String stopValue;

    @JsonProperty("is_active")
    @Schema(description = "Whether the transaction is active (not stopped)")
    private final Boolean isActive;
}
