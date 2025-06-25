package be.cytomine.domain.image.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;

@Entity
@Getter
@Setter
public class ImageGroup extends CytomineDomain {

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @Transient
    private List<Object> images;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ImageGroup imageGroup = this;

        imageGroup.id = json.getJSONAttrLong("id", null);
        imageGroup.created = json.getJSONAttrDate("created");
        imageGroup.updated = json.getJSONAttrDate("updated");

        imageGroup.name = json.getJSONAttrStr("name", null);
        imageGroup.project = (Project) json.getJSONAttrDomain(entityManager, "project", new Project(), true);

        return imageGroup;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ImageGroup imageGroup = (ImageGroup) domain;

        returnArray.put("name", imageGroup.getName());
        returnArray.put("project", imageGroup.getProject().getId());

        List<Object> images = Optional.ofNullable(imageGroup.getImages()).orElse(new ArrayList<>());
        returnArray.put("imageInstances", images);
        returnArray.put("numberOfImages", images.size());

        return returnArray;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    @Override
    public CytomineDomain container() {
        return project.container();
    }
}
