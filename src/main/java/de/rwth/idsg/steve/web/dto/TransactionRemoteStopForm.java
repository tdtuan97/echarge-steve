/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2025 SteVe Community Team
 * All Rights Reserved.
 */
package de.rwth.idsg.steve.web.dto;

import de.rwth.idsg.steve.ocpp.OcppProtocol;
import de.rwth.idsg.steve.ocpp.OcppTransport;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import de.rwth.idsg.steve.web.dto.ocpp.RemoteStopTransactionParams;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 31.08.2015
 */
@Getter
@Setter
@ToString(callSuper = true)
public class TransactionRemoteStopForm extends RemoteStopTransactionParams {
    @Setter
    private OcppTransport ocppTransport;

    @NotBlank(message = "chargeBoxId is required")
    @Setter
    private String chargeBoxId;

    @NotBlank(message = "endpointAddress is required")
    @Setter
    private String endpointAddress;

    @ToString(callSuper = true)
    public static class ForApi extends TransactionRemoteStopForm {
        public ForApi() {
            super();
            setOcppTransport(OcppTransport.JSON);
            setEndpointAddress("-");
            ChargePointSelect chargePoint = new ChargePointSelect(OcppProtocol.V_16_JSON, getChargeBoxId(), getEndpointAddress());
            List<ChargePointSelect> chargePointSelectList = new ArrayList<>(Collections.emptyList());
            chargePointSelectList.add(chargePoint);
            setChargePointSelectList(chargePointSelectList);
        }
    }
}
