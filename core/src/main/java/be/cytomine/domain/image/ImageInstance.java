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
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.UrlApi;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.Date;
import java.util.Optional;

@Entity
@Getter
@Setter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"base_image_id", "project_id" }))
@DiscriminatorColumn(name = "class")
@DiscriminatorValue("be.cytomine.domain.image.ImageInstance")
public class ImageInstance extends CytomineDomain {

    @ManyToOne(fetch = FetchType.EAGER)
    private AbstractImage baseImage;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user; //owner

    private String instanceFilename;

    private Long countImageAnnotations = 0L;

    private Long countImageJobAnnotations = 0L;

    private Long countImageReviewedAnnotations = 0L;

    private Date reviewStart;

    private Date reviewStop;

    @ManyToOne(fetch = FetchType.LAZY)
    private User reviewUser;

    private Integer magnification;

    @Column(name = "physical_size_x")
    private Double physicalSizeX;

    @Column(name = "physical_size_y")
    private Double physicalSizeY;

    @Column(name = "physical_size_z")
    private Double physicalSizeZ;

    private Double fps;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        return buildDomainFromJson(this, json, entityManager);
    }

    public CytomineDomain buildDomainFromJson(ImageInstance imageInstance, JsonObject json, EntityManager entityManager) {
        imageInstance.id = json.getJSONAttrLong("id",null);
        imageInstance.created = json.getJSONAttrDate("created");
        imageInstance.updated = json.getJSONAttrDate("updated");

        imageInstance.user = (User) json.getJSONAttrDomain(entityManager, "user", new User(), false);
        imageInstance.baseImage = (AbstractImage) json.getJSONAttrDomain(entityManager, "baseImage", new AbstractImage(), false);
        imageInstance.project = (Project) json.getJSONAttrDomain(entityManager, "project", new Project(), false);

        imageInstance.instanceFilename = json.getJSONAttrStr("instanceFilename", false);

        imageInstance.reviewStart = json.getJSONAttrDate("reviewStart");
        imageInstance.reviewStop = json.getJSONAttrDate("reviewStart");
        imageInstance.reviewUser = (User) json.getJSONAttrDomain(entityManager, "reviewUser", new User(), false);

        imageInstance.magnification = json.getJSONAttrInteger("magnification", null);
        imageInstance.physicalSizeX = json.getJSONAttrDouble("physicalSizeX", null);
        imageInstance.physicalSizeY = json.getJSONAttrDouble("physicalSizeY", null);
        imageInstance.physicalSizeZ = json.getJSONAttrDouble("physicalSizeZ", null);
        imageInstance.fps = json.getJSONAttrDouble("fps", null);

        //Check review constraint
        if ((imageInstance.reviewUser == null && imageInstance.reviewStart != null)
                || (imageInstance.reviewUser != null && imageInstance.reviewStart == null)
                || (imageInstance.reviewStart == null && imageInstance.reviewStop != null)) {
            throw new WrongArgumentException("Review data are not valid: user= " + this.reviewUser + "start= " + this.reviewStart + " , stop= " + this.reviewStop);
        }

        return imageInstance;
    }


    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ImageInstance imageInstance = (ImageInstance)domain;
        returnArray.put("baseImage", imageInstance.getBaseImageId());
        returnArray.put("project", imageInstance.getProjectId());
        returnArray.put("user", imageInstance.getUserId());

        returnArray.put("instanceFilename", imageInstance.getBlindInstanceFilename());
        returnArray.put("originalFilename", imageInstance.getBlindOriginalFilename());
        returnArray.put("filename", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getFilename).orElse(null));
        returnArray.put("blindedName", imageInstance.getBlindedName());
        returnArray.put("path", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getPath).orElse(null));
        returnArray.put("contentType", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getUploadedFile).map(UploadedFile::getContentType).orElse(null));

        returnArray.put("width", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getWidth).orElse(null));
        returnArray.put("height", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getHeight).orElse(null));
        returnArray.put("depth", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getDepth).orElse(null));  // /!!\ Breaking API : image?.baseImage?.getZoomLevels()?.max
        returnArray.put("duration", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getDuration).orElse(null));
        returnArray.put("channels", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getChannels).orElse(null));
        returnArray.put("extrinsicChannels", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getApparentChannels).orElse(null));



        returnArray.put("physicalSizeX", imageInstance.getPhysicalSizeX());
        returnArray.put("physicalSizeY", imageInstance.getPhysicalSizeY());
        returnArray.put("physicalSizeZ", imageInstance.getPhysicalSizeZ());

        returnArray.put("fps", imageInstance.getFps());
        returnArray.put("zoom", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getZoomLevels).orElse(null));

        returnArray.put("tileSize", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getTileSize).orElse(null));
        returnArray.put("isVirtual", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::isVirtual).orElse(null));

        returnArray.put("magnification", imageInstance.getMagnification());
        returnArray.put("bitPerSample", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getBitPerSample).orElse(null));
        returnArray.put("samplePerPixel", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getSamplePerPixel).orElse(null));

        returnArray.put("colorspace", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getColorspace).orElse(null));


        returnArray.put("reviewStart", Optional.ofNullable(imageInstance.getReviewStart()).map(x -> String.valueOf(x.getTime())).orElse(null));
        returnArray.put("reviewStop", Optional.ofNullable(imageInstance.getReviewStop()).map(x -> String.valueOf(x.getTime())).orElse(null));
        returnArray.put("reviewUser", Optional.ofNullable(imageInstance.getReviewUser()).map(User::getId).orElse(null));


        returnArray.put("reviewed", imageInstance.isReviewed());
        returnArray.put("inReview", imageInstance.isInReviewMode());

        returnArray.put("numberOfAnnotations", imageInstance.countImageAnnotations);
        returnArray.put("numberOfJobAnnotations", imageInstance.countImageJobAnnotations);
        returnArray.put("numberOfReviewedAnnotations", imageInstance.countImageReviewedAnnotations);

        returnArray.put("thumb", UrlApi.getImageInstanceThumbUrlWithMaxSize(imageInstance.id, 512, "png"));
        returnArray.put("preview", UrlApi.getImageInstanceThumbUrlWithMaxSize(imageInstance.id, 1024, "png"));
        returnArray.put("macroURL", UrlApi.getAssociatedImage(imageInstance, "macro", Optional.ofNullable(imageInstance.getBaseImage()).map(AbstractImage::getUploadedFile).map(UploadedFile::getContentType).orElse(null), 512, "png"));

        return returnArray;
    }



    private Long getBaseImageId() {
        return Optional.ofNullable(this.getBaseImage()).map(CytomineDomain::getId).orElse(null);
    }

    private Long getProjectId() {
        return Optional.ofNullable(this.getProject()).map(CytomineDomain::getId).orElse(null);
    }

    private Long getUserId() {
        return Optional.ofNullable(this.getUser()).map(CytomineDomain::getId).orElse(null);
    }

    /**
     * Flag to control if image is beeing review, and not yet validated
     * @return True if image is review but not validate, otherwise false
     */
    public boolean isInReviewMode() {
        return (reviewStart != null && reviewUser != null && reviewStop == null);
    }

    /**
     * Flag to control if image is validated
     * @return True if review user has validate this image
     */
    public boolean isReviewed() {
        return (reviewStop != null);
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project.container();
    }

    /**
     * Return domain user (annotation user, image user...)
     * By default, a domain has no user.
     * You need to override userDomainCreator() in domain class
     * @return Domain user
     */
    @Override
    public User userDomainCreator() {
        return user;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public String getBlindOriginalFilename() {
        if (Optional.ofNullable(project).map(Project::getBlindMode).orElse(false)) {
            return String.valueOf(baseImage.getId());
        }
        return Optional.ofNullable(baseImage).map(AbstractImage::getOriginalFilename).orElse(null);
    }

    public String getBlindInstanceFilename() {
        if (Optional.ofNullable(project).map(Project::getBlindMode).orElse(false)) {
            return String.valueOf(baseImage.getId());
        } else if (instanceFilename!=null && !instanceFilename.trim().isBlank()) {
            return instanceFilename;
        } else {
            return Optional.ofNullable(baseImage).map(AbstractImage::getOriginalFilename).orElse(null);
        }
    }

    public String getBlindedName(){
        if(Optional.ofNullable(project).map(Project::getBlindMode).orElse(false)) {
            return String.valueOf(baseImage.getId());
        }
        return null;
    }

    public Double getPhysicalSizeX() {
        if (physicalSizeX != null && physicalSizeX != 0) {
            return physicalSizeX;
        }
        return Optional.ofNullable(baseImage).map(AbstractImage::getPhysicalSizeX).orElse(null);
    }

    public Double getPhysicalSizeY() {
        if (physicalSizeY != null && physicalSizeY != 0) {
            return physicalSizeY;
        }
        return Optional.ofNullable(baseImage).map(AbstractImage::getPhysicalSizeY).orElse(null);
    }

    public Double getPhysicalSizeZ() {
        if (physicalSizeZ != null && physicalSizeZ != 0) {
            return physicalSizeZ;
        }
        return Optional.ofNullable(baseImage).map(AbstractImage::getPhysicalSizeZ).orElse(null);
    }

    public Double getFps() {
        if (fps != null && fps != 0) {
            return fps;
        }
        return Optional.ofNullable(baseImage).map(AbstractImage::getFps).orElse(null);
    }

    public Integer getMagnification() {
        if (magnification != null && magnification != 0) {
            return magnification;
        }
        return Optional.ofNullable(baseImage).map(AbstractImage::getMagnification).orElse(null);
    }

}
