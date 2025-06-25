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
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.UrlApi;
import be.cytomine.utils.JsonObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
public class ReviewedAnnotation extends AnnotationDomain implements Serializable {

    /**
     * Annotation that has been reviewed (just keep a link)
     */
    Long parentIdent;

    String parentClassName;

    /**
     * Status for the reviewed (not yet use)
     * May be: 'validate','conflict',...
     */
    Integer status;


    /**
     * User that create the annotation that has been reviewed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * User that review annotation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_user_id", nullable = false)
    private User reviewUser;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "reviewed_annotation_term",
            joinColumns = { @JoinColumn(name = "reviewed_annotation_terms_id") },
            inverseJoinColumns = { @JoinColumn(name = "term_id") }
    )
    private List<Term> terms = new ArrayList<>();



    @PrePersist
    public void beforeCreate() {
        super.beforeCreate();
    }

    @PreUpdate
    public void beforeUpdate() {
        super.beforeUpdate();
    }


    /**
     * Set link to the annotation that has been reviewed
     * @param annotation Annotation that is reviewed
     */
    public void putParentAnnotation(AnnotationDomain annotation) {
        parentClassName = annotation.getClass().getName();
        parentIdent = annotation.getId();
    }



    @Override
    public List<Term> terms() {
        return terms;
    }


    /**
     * Get all annotation terms id
     * @return Terms id list
     */
    public List<Long> termsId() {
        return terms().stream().map(CytomineDomain::getId).distinct().collect(Collectors.toList());

    }

    @Override
    public boolean isUserAnnotation() {
        return false;
    }

    @Override
    Long getUserId() {
        return user.getId();
    }

    /**
     * Check if its a review annotation
     */
    public boolean isReviewedAnnotation() {
        return true;
    }

    @Override
    public List<Term> termsForReview() {
        return terms;
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ReviewedAnnotation annotation = this;
        annotation.id = json.getJSONAttrLong("id",null);

        annotation.slice = (SliceInstance)json.getJSONAttrDomain(entityManager, "slice", new SliceInstance(), true);
        annotation.image = (ImageInstance)json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);
        annotation.project = annotation.getImage().getProject();
        annotation.user = (User)json.getJSONAttrDomain(entityManager, "user", new User(), true);
        annotation.reviewUser = (User)json.getJSONAttrDomain(entityManager, "reviewUser", new User(), true);

        annotation.status = json.getJSONAttrInteger("status",0);
        annotation.geometryCompression = json.getJSONAttrDouble("geometryCompression",0D);

        annotation.created = json.getJSONAttrDate("created");
        annotation.updated = json.getJSONAttrDate("updated");


        if (json.containsKey("location") && json.get("location") instanceof Geometry) {
            annotation.location = (Geometry) json.get("location");
        } else {
            try {
                annotation.location = new WKTReader().read(json.getJSONAttrStr("location"));
            }
            catch (ParseException ex) {
                throw new WrongArgumentException(ex.toString());
            }
        }

        if (annotation.location==null) {
            throw new WrongArgumentException("Geometry is null: 0 points");
        }

        if (annotation.location.getNumPoints() < 1) {
            throw new WrongArgumentException("Geometry is empty:" + annotation.location.getNumPoints() + " points");
        }


        /* Parent annotation */
        Long annotationParentId = json.getJSONAttrLong("parentIdent", -1L);
        if (annotationParentId == -1) {
            annotationParentId = json.getJSONAttrLong("annotation", -1L);
        }
        try {
            AnnotationDomain parent = AnnotationDomain.getAnnotationDomain(entityManager, annotationParentId);
            annotation.parentClassName = parent.getClass().getName();
            annotation.parentIdent = parent.getId();
        } catch (Exception ignored) {
            //parent is deleted...
        }

        /* Terms of reviewed annotation */
        if (json.get("terms") == null || (json.containsKey("terms") && json.getJSONAttrStr("terms").equals("null"))) {
            throw new WrongArgumentException("Term list was not found");
        }
        if (!annotation.terms().isEmpty()) {
            annotation.terms.clear(); //remove all review term
        }

        for (Long id : json.getJSONAttrListLong("terms")) {
            Term term = entityManager.find(Term.class, id);
            if (term==null || !term.getOntology().equals(annotation.getProject().getOntology())) {
                throw new WrongArgumentException("Term "+term+" is null or is not in ontology from the annotation project");
            }
            annotation.getTerms().add(term);
        }
        return annotation;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = AnnotationDomain.getDataFromDomain(domain);
        ReviewedAnnotation annotation = (ReviewedAnnotation)domain;

        returnArray.put("parentIdent", annotation.parentIdent);
        returnArray.put("parentClassName", annotation.getParentClassName());
        returnArray.put("status", annotation.getStatus());
        returnArray.put("reviewUser", annotation.getReviewUser().getId());
        returnArray.put("terms", annotation.termsId());
        returnArray.put("term", annotation.termsId());

        returnArray.put("cropURL", UrlApi.getReviewedAnnotationCropWithAnnotationId(annotation.getId(), "png"));
        returnArray.put("smallCropURL", UrlApi.getReviewedAnnotationCropWithAnnotationIdWithMaxSize(annotation.getId(), 256, "png"));
        returnArray.put("url", UrlApi.getReviewedAnnotationCropWithAnnotationId(annotation.getId(), "png"));
        returnArray.put("imageURL", UrlApi.getAnnotationURL(annotation.getImage().getProject().getId(), annotation.getImage().getId(), annotation.getId()));
        returnArray.put("reviewed", true);

        return returnArray;
    }

    @Override
    public User user() {
        return user;
    }

    /**
     * Return domain user (annotation user, image user...)
     * By default, a domain has no user.
     * You need to override userDomainCreator() in domain class
     * @return Domain user
     */
    @Override
    public User userDomainCreator() {
        return reviewUser;
    }
}
