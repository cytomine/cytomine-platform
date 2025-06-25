package be.cytomine.domain.image.server;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;

@Entity
@Getter
@Setter
public class Storage extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false)
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    protected User user;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Storage storage = (Storage)this;
        storage.id = json.getJSONAttrLong("id",null);
        storage.name = json.getJSONAttrStr("name", true);
        storage.user = (User)json.getJSONAttrDomain(entityManager, "user", new User(), true);
        storage.created = json.getJSONAttrDate("created");
        storage.updated = json.getJSONAttrDate("updated");
        return storage;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Storage storage = (Storage)domain;
        returnArray.put("name", storage.getName());
        returnArray.put("user", (storage.getUser()!=null ? storage.getUser().getId() : null));
        return returnArray;
    }

    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public CytomineDomain container() {
        return this;
    }
}
