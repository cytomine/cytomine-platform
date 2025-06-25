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

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;

@Entity
@Getter
@Setter
public class CompanionFile extends CytomineDomain{

    @ManyToOne(fetch = FetchType.EAGER)
    private UploadedFile uploadedFile;

    @ManyToOne(fetch = FetchType.EAGER)
    private AbstractImage image;

    @NotBlank
    private String originalFilename;
    
    @NotBlank
    private String filename;
    
    private  String type;
    
    private  Integer progress = 0;


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        CompanionFile companionFile = this;
        companionFile.id = json.getJSONAttrLong("id",null);
        companionFile.created = json.getJSONAttrDate("created");
        companionFile.updated = json.getJSONAttrDate("updated");

        companionFile.uploadedFile = (UploadedFile) json.getJSONAttrDomain(entityManager, "uploadedFile", new UploadedFile(), false);
        companionFile.image = (AbstractImage) json.getJSONAttrDomain(entityManager, "image", new AbstractImage(), true);


        companionFile.originalFilename = json.getJSONAttrStr("originalFilename", true);
        companionFile.filename = json.getJSONAttrStr("filename", true);
        companionFile.type = json.getJSONAttrStr("type", true);

        companionFile.progress = json.getJSONAttrInteger("progress", 0);

        return companionFile;
    }



    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        CompanionFile companionFile = (CompanionFile)domain;
        returnArray.put("uploadedFile", companionFile.getUploadedFileId());
        returnArray.put("path", companionFile.getPath());
        returnArray.put("image", companionFile.getImageId());
        returnArray.put("originalFilename", companionFile.getOriginalFilename());

        returnArray.put("filename", companionFile.getFilename());
        returnArray.put("type", companionFile.getType());
        returnArray.put("progress", companionFile.getProgress());
        returnArray.put("status", companionFile.getUploadedFileStatus());
        returnArray.put("statusText", companionFile.getUploadedFileStatusText());
        return returnArray;
    }

    public Long getImageId() {
        return this.getImage()!=null ? this.getImage().getId() : null;
    }


    public Long getUploadedFileId() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getId() : null;
    }

    public Integer getUploadedFileStatus() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getStatus() : null;
    }


    public String getUploadedFileStatusText() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getStatusText() : null;
    }

    public String getPath() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getPath() : null;
    }


    @Override
    public CytomineDomain container() {
        return image.container();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

}
