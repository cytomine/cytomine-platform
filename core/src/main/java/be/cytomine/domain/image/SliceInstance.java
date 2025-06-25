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
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Optional;

@Entity
@Getter
@Setter
public class SliceInstance extends CytomineDomain implements Serializable {

    @ManyToOne(fetch = FetchType.EAGER)
    private AbstractSlice baseSlice;

    @ManyToOne(fetch = FetchType.LAZY)
    private ImageInstance image;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        SliceInstance sliceInstance = this;
        sliceInstance.id = json.getJSONAttrLong("id",null);
        sliceInstance.created = json.getJSONAttrDate("created");
        sliceInstance.updated = json.getJSONAttrDate("updated");
        
        sliceInstance.project = (Project)json.getJSONAttrDomain(entityManager, "project", new Project(), true);
        sliceInstance.image = (ImageInstance)json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);
        sliceInstance.baseSlice = (AbstractSlice)json.getJSONAttrDomain(entityManager, "baseSlice", new AbstractSlice(), true);
        
        return sliceInstance;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        SliceInstance sliceInstance = (SliceInstance) domain;
        returnArray.put("uploadedFile", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getUploadedFileId).orElse(null));

        returnArray.put("project", Optional.ofNullable(sliceInstance.project).map(CytomineDomain::getId).orElse(null));
        returnArray.put("baseSlice", Optional.ofNullable(sliceInstance.baseSlice).map(CytomineDomain::getId).orElse(null));
        returnArray.put("image", Optional.ofNullable(sliceInstance.image).map(CytomineDomain::getId).orElse(null));
        returnArray.put("path", sliceInstance.getPath());

        returnArray.put("mime", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getMimeType).orElse(null));
        returnArray.put("channel", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getChannel).orElse(null));
        returnArray.put("zStack", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getZStack).orElse(null));
        returnArray.put("time", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getTime).orElse(null));
        returnArray.put("rank", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getRank).orElse(null));

        returnArray.put("channelName", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getChannelName).orElse(null));
        returnArray.put("channelColor", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getChannelColor).orElse(null));

        returnArray.put("zName", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getZName).orElse(null));

        return returnArray;
    }


    public String getPath() {
        return Optional.ofNullable(this.baseSlice).map(AbstractSlice::getPath).orElse(null);
    }

    public String getMimeType(){
        return Optional.ofNullable(this.baseSlice).map(AbstractSlice::getMimeType).orElse(null);
    }

    @Override
    public CytomineDomain container() {
        return project.container();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }


}
