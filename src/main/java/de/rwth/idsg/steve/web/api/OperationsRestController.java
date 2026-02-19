/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2023 SteVe Community Team
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
package de.rwth.idsg.steve.web.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.ocpp.CommunicationTask;
import de.rwth.idsg.steve.ocpp.OcppProtocol;
import de.rwth.idsg.steve.repository.TaskStore;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import de.rwth.idsg.steve.service.ChargePointServiceClient;
import de.rwth.idsg.steve.service.messaging.RemoteTransactionEventPublisher;
import de.rwth.idsg.steve.web.dto.ocpp.ResetParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Tran Tuan <tdtuan97@gmail.com>
 * @since 13.09.2022
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/operations", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OperationsRestController {

    @Autowired
    private TaskStore taskStore;

    @Autowired
    private ChargePointServiceClient chargePointServiceClient;

    @Autowired
    private RemoteTransactionEventPublisher remoteTransactionEventPublisher;

    // @PostMapping(value = "reset")
    // @ResponseBody
    // public String reset(@RequestBody JSONObject params) {
    //     if (params.get("resetType") == null) {
    //         throw new SteveException("Reset type is required");
    //     }
    //     if (params.get("chargeBoxId") == null) {
    //         throw new SteveException("Charge box ID is required");
    //     }
    //     ResetParams resetParams = new ResetParams();
    //     resetParams.setResetType(ResetType.valueOf(params.get("resetType").toString()));
    //     List<ChargePointSelect> chargePointSelectList = new ArrayList<>();
    //     chargePointSelectList.add(new ChargePointSelect(OcppProtocol.V_16_JSON, params.get("chargeBoxId").toString()));
    //     resetParams.setChargePointSelectList(chargePointSelectList);
    //     Integer taskId = chargePointServiceClient.reset(resetParams);
    //     JSONObject res = new JSONObject();
    //     res.put("status", "success");
    //     res.put("task_id", taskId);
    //     return res.toString();
    // }

    @GetMapping("task/{taskId}")
    @ResponseBody
    public String checkRequest(@PathVariable("taskId") Integer taskId) {
        String chargeBoxId = null;
        JSONObject taskResult = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            CommunicationTask communicationTask = taskStore.get(taskId);
            Map.Entry<String, Object> firstEntry = (Map.Entry<String, Object>) communicationTask.getResultMap().entrySet().iterator().next();
            chargeBoxId = firstEntry.getKey();
            String jsonString = objectMapper.writeValueAsString(firstEntry.getValue());
            Map map = objectMapper.readValue(jsonString, Map.class);
            taskResult = new JSONObject(map);
        } catch (SteveException e) {
            throw new SteveException("Task not found");
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON");
        }
        JSONObject res = new JSONObject();
        res.put("result", taskResult);
        res.put("charge_box_id", chargeBoxId);
        res.put("task_id", taskId);
        try {
            remoteTransactionEventPublisher.publishTaskStatusChecked(taskId, chargeBoxId, taskResult);
        } catch (Exception e) {
            log.warn("Failed to publish task status checked event for task_id={}", taskId, e);
        }
        return res.toString();
    }
}
