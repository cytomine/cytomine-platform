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
import be.cytomine.utils.ClassUtils;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.HashMap;

@Entity
@DiscriminatorValue("be.cytomine.domain.command.EditCommand")
public class EditCommand extends Command {

    public EditCommand(User currentUser, Transaction transaction) {
        this.user = currentUser;
        this.transaction = transaction;
    }

    public EditCommand() {}

    /**
     * Add command info for the new domain concerned by the command
     * @param newObject domain after update
     * @param oldObject domain before update
     * @param message Message build for the command
     */
    protected void fillCommandInfo(CytomineDomain newObject, String oldObject, String message) {
        HashMap<String, Object> paramsData = new HashMap<String, Object>();
        paramsData.put("previous" + ClassUtils.getClassName(newObject), JsonObject.toMap(oldObject));
        paramsData.put("new" + ClassUtils.getClassName(newObject), newObject.toJsonObject());
        data = JsonObject.toJsonString(paramsData);
        actionMessage = message;
    }

    /**
     * Get domain name
     * @return domaine name
     */
    public String domainName() {
        String domain = serviceName.replace("Service", "");
        return domain.substring(0, 1).toUpperCase() + domain.substring(1);
    }

    /**
     * Process an Add operation for this command
     * @return Message
     */
    public CommandResponse execute(ModelService service) {
        //Retrieve domain to update it
        CytomineDomain updatedDomain = this.domain;
        String oldDomain = updatedDomain.toJSON();
        updatedDomain.buildDomainFromJson(json, service.getEntityManager());
        //Init command info TODO
        CytomineDomain container = updatedDomain.container();
        if(container!=null && container instanceof Project) {
            super.setProject((Project)container);
        }
        CommandResponse response = service.edit(updatedDomain, printMessage);
        fillCommandInfo(updatedDomain, oldDomain, (String)response.getData().get("message"));
        return response;
    }
}
