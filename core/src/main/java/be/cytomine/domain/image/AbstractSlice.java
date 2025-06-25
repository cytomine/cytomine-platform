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
import java.util.Optional;

@Entity
@Getter
@Setter
public class AbstractSlice extends CytomineDomain {

    @ManyToOne
    private AbstractImage image;

    @ManyToOne(fetch = FetchType.LAZY)
    private UploadedFile uploadedFile;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private Mime mime; // [PIMS] Deprecated.

    private Integer channel;

    private Integer zStack;

    private Integer time;

    private String channelName;

    private String channelColor;

    private String zName;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AbstractSlice abstractSlice = this;
        abstractSlice.id = json.getJSONAttrLong("id",null);
        abstractSlice.created = json.getJSONAttrDate("created");
        abstractSlice.updated = json.getJSONAttrDate("updated");

        abstractSlice.uploadedFile = (UploadedFile) json.getJSONAttrDomain(entityManager, "uploadedFile", new UploadedFile(), false);
        abstractSlice.image = (AbstractImage) json.getJSONAttrDomain(entityManager, "image", new AbstractImage(), true);
        abstractSlice.mime = (Mime) json.getJSONAttrDomain(entityManager, "mime", new Mime(), "mimeType", "String", false);

        abstractSlice.channel = json.getJSONAttrInteger("channel", 0);
        abstractSlice.zStack = json.getJSONAttrInteger("zStack", 0);
        abstractSlice.time = json.getJSONAttrInteger("time", 0);

        abstractSlice.channelName = json.getJSONAttrStr("channelName", false);
        abstractSlice.channelColor = json.getJSONAttrStr("channelColor", false);

        abstractSlice.zName = json.getJSONAttrStr("zName", false);

        return abstractSlice;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AbstractSlice abstractSlice = (AbstractSlice)domain;
        returnArray.put("uploadedFile", abstractSlice.getUploadedFileId());
        returnArray.put("path", abstractSlice.getPath());
        returnArray.put("image", abstractSlice.getImageId());
        returnArray.put("mime", abstractSlice.getMimeType());

        returnArray.put("channel", abstractSlice.getChannel());
        returnArray.put("zStack", abstractSlice.getZStack());
        returnArray.put("time", abstractSlice.getTime());
        returnArray.put("rank", abstractSlice.getRank());

        returnArray.put("channelName", abstractSlice.getChannelName());
        returnArray.put("channelColor", abstractSlice.getChannelColor());

        returnArray.put("zName", abstractSlice.getZName());

        return returnArray;
    }

    public Long getUploadedFileId() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getId() : null;
    }

    public Long getImageId() {
        return this.getImage()!=null ? this.getImage().getId() : null;
    }

    public String getMimeType() {
        return this.getMime()!=null ? this.getMime().getMimeType() : null;
    }

    public String getPath() {
        UploadedFile referenceUploadedFile = this.getReferenceUploadedFile();
        return referenceUploadedFile!=null ? referenceUploadedFile.getPath() : null;
    }

    public Integer getRank() {
        return this.channel +
                Optional.ofNullable(this.image.getChannels()).orElse(0) *
                        (this.zStack + Optional.ofNullable(this.image.getDepth()).orElse(0) * this.time);
    }

    public UploadedFile getReferenceUploadedFile() {
        if (image!=null && image.isVirtual()) {
            return image.getUploadedFile();
        }
        return uploadedFile;
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
