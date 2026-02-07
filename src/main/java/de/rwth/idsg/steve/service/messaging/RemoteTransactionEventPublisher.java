/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2025 SteVe Community Team
 * All Rights Reserved.
 */
package de.rwth.idsg.steve.service.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes remote transaction API events (remote start, remote stop, force stop, task status)
 * to NATS via ChargePointMessageService. Fire-and-forget; failures are logged and not propagated.
 */
@Slf4j
@Service
public class RemoteTransactionEventPublisher {

    private static final String EVENT_REMOTE_START_REQUESTED = "remote_start_requested";
    private static final String EVENT_REMOTE_STOP_REQUESTED = "remote_stop_requested";
    private static final String EVENT_FORCE_STOP_REQUESTED = "force_stop_requested";
    private static final String EVENT_TASK_STATUS_CHECKED = "task_status_checked";

    private final ChargePointMessageService chargePointMessageService;

    public RemoteTransactionEventPublisher(ChargePointMessageService chargePointMessageService) {
        this.chargePointMessageService = chargePointMessageService;
    }

    /**
     * Publishes remote start requested event after successful API call.
     */
    public void publishRemoteStartRequested(Integer taskId, String chargeBoxId, Integer connectorId, String idTag) {
        if (chargeBoxId == null || chargeBoxId.isBlank()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("task_id", taskId);
        payload.put("charge_box_id", chargeBoxId);
        payload.put("connector_id", connectorId);
        payload.put("id_tag", idTag);
        publish(EVENT_REMOTE_START_REQUESTED, chargeBoxId, payload);
    }

    /**
     * Publishes remote stop requested event after successful API call.
     */
    public void publishRemoteStopRequested(Integer taskId, String chargeBoxId, Integer transactionId) {
        if (chargeBoxId == null || chargeBoxId.isBlank()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("task_id", taskId);
        payload.put("charge_box_id", chargeBoxId);
        payload.put("transaction_id", transactionId);
        publish(EVENT_REMOTE_STOP_REQUESTED, chargeBoxId, payload);
    }

    /**
     * Publishes force stop requested event after successful API call.
     */
    public void publishForceStopRequested(Integer transactionPk) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transaction_id", transactionPk);
        publish(EVENT_FORCE_STOP_REQUESTED, "unknown", payload);
    }

    /**
     * Publishes task status checked event when GET task is called.
     */
    public void publishTaskStatusChecked(Integer taskId, String chargeBoxId, Object result) {
        if (chargeBoxId == null || chargeBoxId.isBlank()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("task_id", taskId);
        payload.put("charge_box_id", chargeBoxId);
        payload.put("result", result);
        publish(EVENT_TASK_STATUS_CHECKED, chargeBoxId, payload);
    }

    private void publish(String event, String chargeBoxId, Map<String, Object> payload) {
        try {
            ChargePointEventPublishParams params = ChargePointEventPublishParams.builder()
                    .tenantType(ChargePointMessageService.DEFAULT_TENANT_TYPE)
                    .tenantCode(ChargePointMessageService.DEFAULT_TENANT_CODE)
                    .chargePointId(chargeBoxId)
                    .event(event)
                    .payload(payload)
                    .build();
            chargePointMessageService.publishChargePointEvent(params);
        } catch (Exception e) {
            log.warn("Failed to publish remote transaction event {} for charge_box_id={}", event, chargeBoxId, e);
        }
    }
}
