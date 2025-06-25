package be.cytomine.domain.ontology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.utils.JsonObject;

@Entity
@Getter
@Setter
public class AnnotationLink extends CytomineDomain {

    @NotNull
    String annotationClassName;

    @NotNull
    @Column(name = "annotation_ident")
    Long annotationIdent;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    AnnotationGroup group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    ImageInstance image;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AnnotationLink annotationLink = this;

        annotationLink.id = json.getJSONAttrLong("id", null);
        annotationLink.created = json.getJSONAttrDate("created");
        annotationLink.updated = json.getJSONAttrDate("updated");

        annotationLink.annotationClassName = json.getJSONAttrStr("annotationClassName", true);
        annotationLink.annotationIdent = json.getJSONAttrLong("annotationIdent", null);
        annotationLink.group = (AnnotationGroup) json.getJSONAttrDomain(entityManager, "group", new AnnotationGroup(), true);
        annotationLink.image = (ImageInstance) json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);

        return annotationLink;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AnnotationLink annotationLink = (AnnotationLink) domain;

        returnArray.put("annotationClassName", annotationLink.getAnnotationClassName());
        returnArray.put("annotationIdent", annotationLink.getAnnotationIdent());
        returnArray.put("group", annotationLink.getGroup().getId());
        returnArray.put("image", annotationLink.getImage().getId());

        return returnArray;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    @Override
    public CytomineDomain container() {
        return group.container();
    }
}
