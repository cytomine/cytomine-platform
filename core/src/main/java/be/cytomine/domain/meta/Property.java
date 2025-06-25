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

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.GenericCytomineDomainContainer;
import be.cytomine.utils.JsonObject;

@Entity
@Getter
@Setter
public class Property extends CytomineDomain {

    @NotNull
    @NotBlank
    private String key;

    @NotBlank
    private String value;

    @NotNull
    @NotBlank
    private String domainClassName;

    @NotNull
    private Long domainIdent;

    /**
     * Set annotation (storing class + id)
     * With groovy, you can do: this.annotation = ...
     * @param domain to add
     */
    public void setDomain(CytomineDomain domain) {
        domainClassName = domain.getClass().getName();
        domainIdent = domain.getId();
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Property property = (Property)this;
        property.id = json.getJSONAttrLong("id",null);

        Long id = json.getJSONAttrLong("domainIdent",-1l);
        if (id == -1) {
            id = json.getJSONAttrLong("domain",-1l);
        }
        property.domainIdent = id;
        property.domainClassName = json.getJSONAttrStr("domainClassName", true);
        property.key = json.getJSONAttrStr("key", true);
        property.value = json.getJSONAttrStr("value", true);
        property.created = json.getJSONAttrDate("created");
        property.updated = json.getJSONAttrDate("updated");
        return property;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Property property = (Property)domain;
        returnArray.put("domainIdent", property.getDomainIdent());
        returnArray.put("domainClassName", property.getDomainClassName());
        returnArray.put("key", property.getKey());
        returnArray.put("value", property.getValue());
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

    public CytomineDomain container() {
        GenericCytomineDomainContainer genericCytomineDomainContainer = new GenericCytomineDomainContainer();
        genericCytomineDomainContainer.setId(domainIdent);
        genericCytomineDomainContainer.setContainerClass(domainClassName);
        return genericCytomineDomainContainer;
    }
}
