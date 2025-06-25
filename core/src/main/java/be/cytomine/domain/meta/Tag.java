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
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class Tag extends CytomineDomain {

    @NotNull
    @NotBlank
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    protected User user;


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Tag tag = (Tag)this;
        tag.setId(json.getJSONAttrLong("id",null));
        tag.setName(json.getJSONAttrStr("name", true));
        tag.setUser((User) json.getJSONAttrDomain(entityManager, "user", new User(), true));
        return tag;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Tag tag = (Tag)domain;
        returnArray.put("name", tag.getName());
        returnArray.put("user", tag.getUser().getId());
        returnArray.put("creatorName", tag.getUser().getUsername());
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

    @Override
    public User userDomainCreator() {
        return user;
    }

}
