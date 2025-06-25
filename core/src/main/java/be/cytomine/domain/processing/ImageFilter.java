package be.cytomine.domain.processing;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.Optional;

@Entity
@Getter
@Setter
public class ImageFilter extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    String method;

    Boolean available = true;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ImageFilter processingServer = (ImageFilter)this;
        processingServer.id = json.getJSONAttrLong("id",null);
        processingServer.name = json.getJSONAttrStr("name", true);
        processingServer.method = json.getJSONAttrStr("method", null);
        processingServer.available = json.getJSONAttrBoolean("available", true);
        processingServer.created = json.getJSONAttrDate("created");
        processingServer.updated = json.getJSONAttrDate("updated");
        return processingServer;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ImageFilter processingServer = (ImageFilter)domain;
        returnArray.put("name", processingServer.getName());
        returnArray.put("method", processingServer.getMethod());
        returnArray.put("available", processingServer.getAvailable());
        returnArray.put("baseUrl", processingServer.getBaseUrl());
        return returnArray;
    }


    public String getBaseUrl() {
        return "/filters/"+method+"&url="; // For backwards compatibility
    }

    @Override
    public String toJSON() {
        return toJsonObject().toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

}
