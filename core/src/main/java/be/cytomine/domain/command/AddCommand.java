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

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Cytomine Team
 * The AddCommand class is a command that create a new domain
 * It provide an execute method that create domain from command, an undo method that drop domain and an redo method that recreate domain
 */
@Entity
@DiscriminatorValue("be.cytomine.domain.command.AddCommand")
public class AddCommand extends Command {

    public AddCommand(User currentUser) {
        this.user = currentUser;
    }

    public AddCommand(User currentUser, Transaction transaction) {
        this.user = currentUser;
        this.transaction = transaction;
    }

    public AddCommand() {
        super();
    }

    /**
     * Process an Add operation for this command
     * @return Message
     */
    public CommandResponse execute(ModelService service) {
        //Create new domain from json data
        json.put("id", null);
        CytomineDomain newDomain = service.createFromJSON(json);
        //Save new domain in database
        CommandResponse response = service.create(newDomain, printMessage);
        //Init command domain
        newDomain = response.getObject();
        fillCommandInfo(newDomain, (String)response.getData().get("message"));
        if (newDomain != null) {
            CytomineDomain container = newDomain.container();
            if(container!=null && container instanceof Project) {
                super.setProject((Project)container);
            }
        }

        return response;
    }
}
