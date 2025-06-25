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
import be.cytomine.domain.GenericCytomineDomainContainer;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class TagDomainAssociation extends CytomineDomain {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tag_id")
    protected Tag tag;

    @NotNull
    @NotBlank
    private String domainClassName;

    @NotNull
    private Long domainIdent;

    public void setDomain(CytomineDomain domain) {
        domainClassName = domain.getClass().getName();
        domainIdent = domain.getId();
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        TagDomainAssociation tagDomainAssocitation = (TagDomainAssociation)this;
        tagDomainAssocitation.setId(json.getJSONAttrLong("id",null));
        tagDomainAssocitation.setDomainIdent(json.getJSONAttrLong("domainIdent",-1l));
        tagDomainAssocitation.setDomainClassName(json.getJSONAttrStr("domainClassName"));
        tagDomainAssocitation.setTag((Tag) json.getJSONAttrDomain(entityManager, "tag", new Tag(), true));
        return tagDomainAssocitation;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        TagDomainAssociation tagDomainAssocitation = (TagDomainAssociation)domain;
        returnArray.put("domainIdent", tagDomainAssocitation.getDomainIdent());
        returnArray.put("domainClassName", tagDomainAssocitation.getDomainClassName());
        returnArray.put("tag", tagDomainAssocitation.getTag().getId());
        returnArray.put("tagName", tagDomainAssocitation.getTag().getName());
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
