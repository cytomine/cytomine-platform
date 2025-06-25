package be.cytomine.domain.meta;

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

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Entity
@Getter
@Setter
public class Configuration extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    @Pattern(regexp = "^[^.]+$") // any char except a dot
    private String key;

    @NotNull
    @NotBlank
    private String value;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ConfigurationReadingRole readingRole;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Configuration configuration = (Configuration)this;
        configuration.id = json.getJSONAttrLong("id",null);
        configuration.key = json.getJSONAttrStr("key", true);
        configuration.value = json.getJSONAttrStr("value", true);
        configuration.readingRole = ConfigurationReadingRole.valueOf(json.getJSONAttrStr("readingRole",true));
        configuration.created = json.getJSONAttrDate("created");
        configuration.updated = json.getJSONAttrDate("updated");
        return configuration;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Configuration configuration = (Configuration)domain;
        returnArray.put("key", configuration.getKey());
        returnArray.put("value", configuration.getValue());
        returnArray.put("readingRole", configuration.getReadingRole().name());
        return returnArray;
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
