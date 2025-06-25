package be.cytomine.domain.image;

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
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

@Entity
@Getter
@Setter
@DiscriminatorValue("be.cytomine.domain.image.NestedImageInstance")
public class NestedImageInstance extends ImageInstance {

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private ImageInstance parent;

    @NotNull
    private Integer x;

    @NotNull
    private Integer y;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        NestedImageInstance nestedImageInstance = (NestedImageInstance)super.buildDomainFromJson(this, json, entityManager);
        nestedImageInstance.parent = (ImageInstance)json.getJSONAttrDomain(entityManager, "parent", new ImageInstance(), true);
        nestedImageInstance.x = json.getJSONAttrInteger("x",0);
        nestedImageInstance.y = json.getJSONAttrInteger("y",0);
        return nestedImageInstance;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = ImageInstance.getDataFromDomain(domain);
        NestedImageInstance nestedImageInstance = (NestedImageInstance) domain;
        returnArray.put("parent", nestedImageInstance.getParentId());
        returnArray.put("x", nestedImageInstance.getX());
        returnArray.put("y", nestedImageInstance.getY());
        return returnArray;
    }

    public Long getParentId() {
        return Optional.ofNullable(parent).map(CytomineDomain::getId).orElse(null);
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

}
