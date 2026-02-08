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
import de.rwth.idsg.steve.repository.dto.OcppTag.OcppTagOverview;
import de.rwth.idsg.steve.service.OcppTagService;
import de.rwth.idsg.steve.web.dto.OcppTagForm;
import de.rwth.idsg.steve.web.dto.OcppTagQueryForm.OcppTagQueryFormForApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for ID tag (Ocpp Tag): list, get by idTag, create, update.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/tags", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TagRestController {

    private final OcppTagService ocppTagService;

    @GetMapping(value = "")
    public List<OcppTagOverview> get() {
        OcppTagQueryFormForApi params = new OcppTagQueryFormForApi();
        return ocppTagService.getOverview(params);
    }

    @GetMapping("/{idTag}")
    public OcppTagOverview getOne(@PathVariable("idTag") String idTag) {
        return getOneByIdTagInternal(idTag);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OcppTagOverview create(@RequestBody @Valid OcppTagForm params) {
        log.debug("Create tag request: {}", params);
        int ocppTagPk = ocppTagService.addOcppTag(params);
        OcppTagOverview response = getOneInternal(ocppTagPk);
        log.debug("Create tag response: {}", response);
        return response;
    }

    @PutMapping("/{idTag}")
    public OcppTagOverview update(@PathVariable("idTag") String idTag, @RequestBody @Valid OcppTagForm params) {
        OcppTagOverview existing = getOneByIdTagInternal(idTag);
        params.setOcppTagPk(existing.getOcppTagPk());
        params.setIdTag(idTag);
        log.debug("Update tag request: {}", params);
        ocppTagService.updateOcppTag(params);
        OcppTagOverview response = getOneByIdTagInternal(idTag);
        log.debug("Update tag response: {}", response);
        return response;
    }

    private OcppTagOverview getOneInternal(int ocppTagPk) {
        OcppTagQueryFormForApi params = new OcppTagQueryFormForApi();
        params.setOcppTagPk(ocppTagPk);
        List<OcppTagOverview> results = ocppTagService.getOverview(params);
        if (results.isEmpty()) {
            throw new SteveException.NotFound("Could not find tag with ocppTagPk=" + ocppTagPk);
        }
        return results.get(0);
    }

    private OcppTagOverview getOneByIdTagInternal(String idTag) {
        OcppTagQueryFormForApi params = new OcppTagQueryFormForApi();
        params.setIdTag(idTag);
        List<OcppTagOverview> results = ocppTagService.getOverview(params);
        if (results.isEmpty()) {
            throw new SteveException.NotFound("Could not find tag with idTag=" + idTag);
        }
        return results.get(0);
    }
}
