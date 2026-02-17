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
package de.rwth.idsg.steve.web.api;

import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.service.ChargePointService;
import de.rwth.idsg.steve.utils.mapper.ChargePointDetailsMapper;
import de.rwth.idsg.steve.web.dto.Address;
import de.rwth.idsg.steve.web.dto.ApiChargePointResponse;
import de.rwth.idsg.steve.web.dto.ChargePointForm;
import de.rwth.idsg.steve.web.dto.ChargePointQueryForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for charge points: list, get by chargeBoxId, create, update, delete.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/charge-points", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ChargePointRestController {

    private final ChargePointService chargePointService;

    @GetMapping(value = "")
    public List<ApiChargePointResponse> get() {
        ChargePointQueryForm params = new ChargePointQueryForm();
        List<ChargePoint.Overview> overviews = chargePointService.getOverview(params);
        return overviews.stream()
                .map(overview -> toResponse(getDetailsInternal(overview.getChargeBoxPk())))
                .collect(Collectors.toList());
    }

    @GetMapping("/{chargeBoxId}")
    public ApiChargePointResponse getOne(@PathVariable("chargeBoxId") String chargeBoxId) {
        ChargePoint.Overview overview = getOneByChargeBoxIdInternal(chargeBoxId);
        return toResponse(getDetailsInternal(overview.getChargeBoxPk()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiChargePointResponse create(@RequestBody @Valid ChargePointForm params) {
        ensureAddressPresent(params);
        log.debug("Create charge point request: {}", params);
        int chargeBoxPk = chargePointService.addChargePoint(params);
        ApiChargePointResponse response = toResponse(getDetailsInternal(chargeBoxPk));
        log.debug("Create charge point response: {}", response);
        return response;
    }

    @PutMapping("/{chargeBoxId}")
    public ApiChargePointResponse update(@PathVariable("chargeBoxId") String chargeBoxId,
                                         @RequestBody @Valid ChargePointForm params) {
        ensureAddressPresent(params);
        ChargePoint.Overview existing = getOneByChargeBoxIdInternal(chargeBoxId);
        params.setChargeBoxPk(existing.getChargeBoxPk());
        params.setChargeBoxId(chargeBoxId);
        log.debug("Update charge point request: {}", params);
        chargePointService.updateChargePoint(params);
        ApiChargePointResponse response = toResponse(getDetailsInternal(existing.getChargeBoxPk()));
        log.debug("Update charge point response: {}", response);
        return response;
    }

    @DeleteMapping("/{chargeBoxId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("chargeBoxId") String chargeBoxId) {
        ChargePoint.Overview existing = getOneByChargeBoxIdInternal(chargeBoxId);
        chargePointService.deleteChargePoint(existing.getChargeBoxPk());
    }

    private void ensureAddressPresent(ChargePointForm params) {
        if (params.getAddress() == null) {
            params.setAddress(new Address());
        }
    }

    private ApiChargePointResponse toResponse(ChargePoint.Details details) {
        return ChargePointDetailsMapper.mapToApiResponse(details);
    }

    private ChargePoint.Details getDetailsInternal(int chargeBoxPk) {
        return chargePointService.getDetails(chargeBoxPk);
    }

    private ChargePoint.Overview getOneInternal(int chargeBoxPk) {
        ChargePointQueryForm params = new ChargePointQueryForm();
        params.setChargeBoxPk(chargeBoxPk);
        List<ChargePoint.Overview> results = chargePointService.getOverview(params);
        if (results.isEmpty()) {
            throw new SteveException.NotFound("Could not find charge point with chargeBoxPk=" + chargeBoxPk);
        }
        return results.get(0);
    }

    private ChargePoint.Overview getOneByChargeBoxIdInternal(String chargeBoxId) {
        var registration = chargePointService.getRegistrationDirect(chargeBoxId);
        if (registration.isEmpty()) {
            throw new SteveException.NotFound("Could not find charge point with chargeBoxId=" + chargeBoxId);
        }
        return getOneInternal(registration.get().chargeBoxPk());
    }
}
