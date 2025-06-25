package be.cytomine.domain.command;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;

@Entity
@Getter
@Setter
public class RedoStackItem extends CytomineDomain {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "command_id", nullable = false)
    protected Command command;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    protected User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = true)
    protected Transaction transaction;

    public RedoStackItem() { }

    public RedoStackItem(UndoStackItem firstUndoStack) {
        this.command = firstUndoStack.getCommand();
        this.user = firstUndoStack.getUser();
        this.transaction = firstUndoStack.getTransaction();
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        throw new RuntimeException("Not supported");
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
