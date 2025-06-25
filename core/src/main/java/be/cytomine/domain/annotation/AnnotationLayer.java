package be.cytomine.domain.annotation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;

@Setter
@Getter
@Entity
public class AnnotationLayer extends CytomineDomain {

    @NotBlank
    @Column(nullable = false)
    private String name;

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        AnnotationLayer annotationLayer = (AnnotationLayer) domain;
        JsonObject domainData = CytomineDomain.getDataFromDomain(domain);
        domainData.put("name", annotationLayer.getName());

        return domainData;
    }

    @Override
    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AnnotationLayer annotationLayer = this;
        annotationLayer.id = json.getJSONAttrLong("id", null);
        annotationLayer.name = json.getJSONAttrStr("name", true);
        annotationLayer.created = json.getJSONAttrDate("created");
        annotationLayer.updated = json.getJSONAttrDate("updated");

        return annotationLayer;
    }

    @Override
    public CytomineDomain container() {
        return this;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    @Override
    public String toString() {
        return String.format("AnnotationLayer{id=%d, name='%s'}", id, name);
    }
}
