/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2025 SteVe Community Team
 * All Rights Reserved.
 */
package de.rwth.idsg.steve.service.messaging;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Parameters for publishing a charge point event to NATS JetStream.
 */
@Getter
@Builder
public class ChargePointEventPublishParams {
    private final String tenantType;
    private final String tenantCode;
    private final String customerCode;
    private final Long customerId;
    private final String chargePointId;
    private final String event;
    private final Map<String, Object> payload;
}
