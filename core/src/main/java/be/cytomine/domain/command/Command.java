package be.cytomine.domain.command;

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
import be.cytomine.service.ModelService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="class",
        discriminatorType = DiscriminatorType.STRING)
public abstract class Command extends CytomineDomain {

    /**
     * JSON string with relevant field data
     */
    @Column(nullable = true)
    protected String data;

    /**
     * JSON object with data
     */
    @Transient
    protected JsonObject json; //TODO: support json array

    @Transient
    protected CytomineDomain domain;

    /**
     * User who launch command
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = true)
    protected User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", nullable = true)
    protected Transaction transaction;

    /**
     * Project concerned by command
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = true)
    protected Project project;

    /**
     * Flag that indicate that the message will be show or not for undo/redo
     */
    protected boolean printMessage = true;

    /**
     * Message explaining the command
     */
    @Column(nullable = true)
    protected String actionMessage;

    /**
     * Set to false if command is not undo(redo)-able
     * By default, don't save command on stack
     */
    protected boolean saveOnUndoRedoStack = false;

    /**
     * Service name of the relevant domain for the command
     */
    @Column(nullable = true)
    protected String serviceName;

    /**
     * If command is save on undo stack, refuse undo
     * Usefull for project delete (cannot undo)
     */
    protected boolean refuseUndo = false;

    public String toString() {
        return this.getClass().getSimpleName() +" "+this.id + "[" + this.created + "]";
    }

    /**
     * Add command info for the new domain concerned by the command
     * @param newObject New domain
     * @param message Message build for the command
     */
    protected void fillCommandInfo(CytomineDomain newObject, String message) {
        data = newObject.toJSON();
        actionMessage = message;
    }

    /**
     * Add command info for the new domain concerned by the command
     * @param newObject New json domain
     * @param message Message build for the command
     */
    protected void fillCommandInfoJSON(String newObject, String message) {
        data = newObject;
        actionMessage = message;
    }

    public abstract CommandResponse execute(ModelService service);

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Command command = (Command)domain;
        returnArray.put("CLASSNAME", domain.getClass().getSimpleName());
        returnArray.put("serviceName", ((Command) domain).getServiceName());
        returnArray.put("action", command.getActionMessage()  + " by " + (command.getUser() != null ? command.getUser().getUsername() : ""));
        returnArray.put("data", command.getData());
        returnArray.put("user", command.getUser().getId());
        String type = "UNKNOWN";
        if (domain instanceof AddCommand) {
            type = "ADD";
        } else if (domain instanceof EditCommand) {
            type = "EDIT";
        } else if (domain instanceof DeleteCommand) {
            type = "DELETE";
        }
        returnArray.put("type", type);
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
}
