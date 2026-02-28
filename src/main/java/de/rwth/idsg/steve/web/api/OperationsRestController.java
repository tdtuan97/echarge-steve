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


import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.ocpp.CommunicationTask;
import de.rwth.idsg.steve.ocpp.RequestResult;
import de.rwth.idsg.steve.repository.TaskStore;
import de.rwth.idsg.steve.service.ChargePointServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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


    private static final String DATA_SEPARATOR = " / Data: ";

    @GetMapping("task/{taskId}")
    @ResponseBody
    public String checkRequest(@PathVariable("taskId") Integer taskId) {
        String chargeBoxId;
        RequestResult requestResult;
        try {
            CommunicationTask communicationTask = taskStore.get(taskId);
            Map.Entry<String, RequestResult> firstEntry = communicationTask.getResultMap().entrySet().iterator().next();
            chargeBoxId = firstEntry.getKey();
            requestResult = firstEntry.getValue();
        } catch (SteveException e) {
            throw new SteveException("Task not found");
        }

        // Parse the response string: "Accepted / Data: {json}" or just "Accepted"
        String responseStr = requestResult.getResponse();
        String status = responseStr;
        JSONObject result = null;

        if (responseStr != null) {
            result = new JSONObject();
            int dataIdx = responseStr.indexOf(DATA_SEPARATOR);
            if (dataIdx >= 0) {
                status = responseStr.substring(0, dataIdx);
                result.put("status", status);
                result.put("data", responseStr.substring(dataIdx + DATA_SEPARATOR.length()));
            } else {
                result.put("status", responseStr);
            }
        }

        JSONObject res = new JSONObject();
        res.put("status", status);
        res.put("errorMessage", requestResult.getErrorMessage());
        res.put("result", result);
        res.put("task_id", taskId);
        res.put("charge_box_id", chargeBoxId);
        return res.toString();
    }
}
