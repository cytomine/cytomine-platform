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
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.LTreeType;
import be.cytomine.utils.LongArrayToBytesConverter;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
public class UploadedFile extends CytomineDomain implements Serializable {

    public static Set<String> ARCHIVE_FORMATS = Set.of("ZIP", "TAR", "GZTAR", "BZTAR", "XZTAR");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_id", nullable = true)
    private Storage storage;

    @Convert(converter = LongArrayToBytesConverter.class)
    private Long[] projects;

    private String filename;

    private String originalFilename;

    private String ext;

    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private UploadedFile parent;

    private Long size;

    private int status = 0;

    @Type(LTreeType.class)
    private String lTree;

    @Override
    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        UploadedFile uploadedFile = this;
        uploadedFile.id = json.getJSONAttrLong("id",null);
        uploadedFile.created = json.getJSONAttrDate("created");
        uploadedFile.updated = json.getJSONAttrDate("updated");
        uploadedFile.user = (User) json.getJSONAttrDomain(entityManager, "user", new User(), true);

        uploadedFile.parent = (UploadedFile)json.getJSONAttrDomain(entityManager, "parent", new UploadedFile(), false);
        uploadedFile.storage = (Storage)json.getJSONAttrDomain(entityManager, "storage", new Storage(), true);

        uploadedFile.filename = json.getJSONAttrStr("filename");
        uploadedFile.originalFilename = json.getJSONAttrStr("originalFilename");
        uploadedFile.ext = json.getJSONAttrStr("ext");
        uploadedFile.contentType = json.getJSONAttrStr("contentType");
        uploadedFile.size = json.getJSONAttrLong("size", 0L);
        uploadedFile.status = json.getJSONAttrInteger("status", 0);
        uploadedFile.projects = json.isMissing("projects") ? null : json.getJSONAttrListLong("projects").toArray(new Long[0]);

        return uploadedFile;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        UploadedFile uploadedFile = (UploadedFile)domain;
        returnArray.put("user", (uploadedFile.getUser()!=null ? uploadedFile.getUser().getId() : null));
        returnArray.put("parent", (uploadedFile.getParent()!=null ? uploadedFile.getParent().getId() : null));
        returnArray.put("storage", (uploadedFile.getStorage()!=null ? uploadedFile.getStorage().getId() : null));
        returnArray.put("originalFilename", uploadedFile.getOriginalFilename());
        returnArray.put("filename", uploadedFile.getFilename());
        returnArray.put("ext", uploadedFile.getExt());
        returnArray.put("contentType",uploadedFile.getContentType());
        returnArray.put("size",uploadedFile.getSize());
        returnArray.put("path",uploadedFile.getPath());
        returnArray.put("isArchive",uploadedFile.isArchive());
        returnArray.put("status",uploadedFile.getStatus());
        returnArray.put("statusText",uploadedFile.getStatusText());
        returnArray.put("projects", uploadedFile.getProjects());
        return returnArray;
    }

    String getStatusText() {
        return UploadedFileStatus.findByCode(status).map(Enum::name).orElse(null);
    }

    public String getPath() {
        // //TODO: use a directory per storage
        return filename;
    }

    boolean isArchive() {
        return ARCHIVE_FORMATS.contains(contentType);
    }

    @PreUpdate
    public void beforeUpdate() {
        updateLtree();
    }

    public void updateLtree() {
        lTree = parent != null ? parent.getLTree()+"." : "";
        lTree += id;
    }

    public CytomineDomain container() {
        return storage;
    }

    @Override
    public String toJSON() {
        return toJsonObject().toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public boolean isVirtual() {
        return contentType.equalsIgnoreCase("VIRTUALSTACK");
    }


}
