package be.cytomine.domain.ontology;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class Track extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false)
    private String name;

    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = true)
    private ImageInstance image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;
    
    
    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Track track = this;
        track.id = json.getJSONAttrLong("id",null);
        track.name = json.getJSONAttrStr("name");
        track.color = json.getJSONAttrStr("color");

        track.image = (ImageInstance)json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);
        track.project = (Project)json.getJSONAttrDomain(entityManager, "project", new Project(), true);

        track.created = json.getJSONAttrDate("created");
        track.updated = json.getJSONAttrDate("updated");
        return track;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Track track = (Track)domain;
        returnArray.put("name", track.getName());
        returnArray.put("color", track.getColor());
        returnArray.put("image", track.getImage()!=null ? track.getImage().getId() : null);
        returnArray.put("project", track.getProject()!=null ? track.getProject().getId() : null);
        return returnArray;
    }

    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public CytomineDomain container() {
        return image.container();
    }
}
