package be.cytomine.domain.image.group;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.utils.JsonObject;

@Entity
@Getter
@Setter
public class ImageGroupImageInstance extends CytomineDomain {

    @ManyToOne(fetch = FetchType.LAZY)
    private ImageGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    private ImageInstance image;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ImageGroupImageInstance igii = this;

        igii.id = json.getJSONAttrLong("id", null);
        igii.created = json.getJSONAttrDate("created");
        igii.updated = json.getJSONAttrDate("updated");

        igii.group = (ImageGroup) json.getJSONAttrDomain(entityManager, "group", new ImageGroup(), true);
        igii.image = (ImageInstance) json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);

        return igii;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ImageGroupImageInstance igii = (ImageGroupImageInstance) domain;

        returnArray.put("image", igii.getImage().getId());
        returnArray.put("group", igii.getGroup().getId());
        returnArray.put("groupName", igii.getGroup().getName());

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
