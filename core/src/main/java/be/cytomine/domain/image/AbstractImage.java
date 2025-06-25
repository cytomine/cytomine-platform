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
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.UrlApi;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@Setter
public class AbstractImage extends CytomineDomain {

    @ManyToOne(fetch = FetchType.EAGER)
    private UploadedFile uploadedFile;

    private String originalFilename;

    @Min(1)
    private Integer width;

    @Min(1)
    private Integer height;

    @Min(1)
    private Integer depth = 1;

    @Min(1)
    private Integer duration = 1;

    @Min(1)
    private Integer channels = 1; // [PIMS] Intrinsic number of channels (RGB image = 1 intrinsic channel)

    private Integer extrinsicChannels;  // [PIMS] True number of (color) channels (RGB image = 3 extrinsic channels)
    // TODO: in a new API, should be renamed to "channels"

    private Integer samplePerPixel = 8;

    private Integer bitPerSample;

    @Column(name = "physical_size_x")
    private Double physicalSizeX;

    @Column(name = "physical_size_y")
    private Double physicalSizeY;

    @Column(name = "physical_size_z")
    private Double physicalSizeZ;

    private Double fps;

    private Integer magnification;

    // TODO: Remove, no more filled by [PIMS]
    private String colorspace;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user; //owner

    @Transient
    private Boolean inProject = false;

    private Integer tileSize = 256;


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AbstractImage abstractImage = this;
        abstractImage.id = json.getJSONAttrLong("id",null);
        abstractImage.created = json.getJSONAttrDate("created");
        abstractImage.updated = json.getJSONAttrDate("updated");

        abstractImage.originalFilename = json.getJSONAttrStr("originalFilename");
        if (originalFilename!=null && originalFilename.trim().equals("")) {
            throw new WrongArgumentException("'originalFilename' property cannot be blank");
        }
        abstractImage.uploadedFile = (UploadedFile) json.getJSONAttrDomain(entityManager, "uploadedFile", new UploadedFile(), false);

        abstractImage.height = json.getJSONAttrInteger("height", null);
        abstractImage.width = json.getJSONAttrInteger("width", null);
        abstractImage.depth = json.getJSONAttrInteger("depth", 1);
        abstractImage.duration = json.getJSONAttrInteger("duration", 1);
        abstractImage.channels = json.getJSONAttrInteger("channels", 1);

        abstractImage.samplePerPixel = json.getJSONAttrInteger("samplePerPixel", 1);
        abstractImage.bitPerSample = json.getJSONAttrInteger("bitPerSample", 8);

        abstractImage.physicalSizeX = json.getJSONAttrDouble("physicalSizeX", null);
        abstractImage.physicalSizeY = json.getJSONAttrDouble("physicalSizeY", null);
        abstractImage.physicalSizeZ = json.getJSONAttrDouble("physicalSizeZ", null);

        abstractImage.fps = json.getJSONAttrDouble("fps", null);

        abstractImage.magnification = json.getJSONAttrInteger("magnification", null);
        abstractImage.colorspace = json.getJSONAttrStr("colorspace", false);

        abstractImage.tileSize = json.getJSONAttrInteger("tileSize", 256);

        return abstractImage;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AbstractImage abstractImage = (AbstractImage)domain;
        returnArray.put("filename", abstractImage.getFilename());
        returnArray.put("originalFilename", abstractImage.getOriginalFilename());
        returnArray.put("uploadedFile", abstractImage.getUploadedFileId());
        returnArray.put("path", abstractImage.getPath());
        returnArray.put("contentType", abstractImage.getContentType());

        returnArray.put("width", abstractImage.getWidth());
        returnArray.put("height", abstractImage.getHeight());
        returnArray.put("depth", abstractImage.getDepth());
        returnArray.put("duration", abstractImage.getDuration());
        returnArray.put("channels", abstractImage.getChannels());
        returnArray.put("extrinsicChannels", abstractImage.getExtrinsicChannels());
        returnArray.put("dimensions", abstractImage.getDimensions());
        returnArray.put("apparentChannels", abstractImage.getExtrinsicChannels());

        returnArray.put("physicalSizeX", abstractImage.getPhysicalSizeX());
        returnArray.put("physicalSizeY", abstractImage.getPhysicalSizeY());
        returnArray.put("physicalSizeZ", abstractImage.getPhysicalSizeZ());

        returnArray.put("fps", abstractImage.getFps());
        returnArray.put("zoom", abstractImage.getZoomLevels());  // /!!\ Breaking API : image?.getZoomLevels()?.max

        returnArray.put("tileSize", abstractImage.getTileSize());
        returnArray.put("isVirtual", abstractImage.isVirtual());

        returnArray.put("magnification", abstractImage.getMagnification());
        returnArray.put("bitPerSample", abstractImage.getBitPerSample());
        returnArray.put("samplePerPixel", abstractImage.getSamplePerPixel());
        returnArray.put("colorspace", abstractImage.getColorspace());
        returnArray.put("thumb", UrlApi.getAbstractImageThumbUrlWithMaxSize(abstractImage.id, 512, "png"));
        returnArray.put("preview", UrlApi.getAbstractImageThumbUrlWithMaxSize(abstractImage.id, 1024, "png"));
        returnArray.put("macroURL", UrlApi.getAssociatedImage(abstractImage, "macro", Optional.ofNullable(abstractImage.getUploadedFile()).map(UploadedFile::getContentType).orElse(null), 512, "png"));

        returnArray.put("inProject", abstractImage.getInProject());
        return returnArray;
    }

    public int getApparentChannels() {
        return channels * samplePerPixel;
    }

    public boolean isVirtual() {
        return uploadedFile!=null? uploadedFile.isVirtual() : false;
    }


    public Long getUploadedFileId() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getId() : null;
    }

    public String getContentType() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getContentType() : null;
    }

    public String getPath() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getPath() : null;
    }

    public Integer getZoomLevels() {
        if (width==null || height==null) {
            return 1;
        }

        double tmpWidth = width;
        double tmpHeight = height;
        int nbZoom = 0;
        while (tmpWidth > tileSize || tmpHeight > tileSize) {
            nbZoom++;
            tmpWidth /= 2;
            tmpHeight /= 2;
        }
        return nbZoom;
    }

    String getFilename() {
        return originalFilename;
    }

    public String getDimensions() {
        List<String> dimensions = new ArrayList<>();
        dimensions.add("X");
        dimensions.add("Y");
        if (channels!=null && channels > 1) {
            dimensions.add("C");
        }
        if (depth!=null && depth > 1) {
            dimensions.add("Z");
        }
        if (duration!=null && duration > 1) {
            dimensions.add("T");
        }
        return String.join("", dimensions);
    }


    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        if (uploadedFile != null) {
            return uploadedFile.container();
        }
        return null;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }



}
