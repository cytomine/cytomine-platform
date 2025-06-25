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
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

@Getter
@Setter
@Entity
@DiscriminatorValue("be.cytomine.domain.command.DeleteCommand")
public class DeleteCommand extends Command {



    public DeleteCommand(User currentUser, Transaction transaction) {
        this.user = currentUser;
        this.transaction = transaction;
    }

    public DeleteCommand() {}

    @Transient
    Object backup;

    /**
     * Add project link in command
     */
    boolean linkProject = true;

    /**
     * Process an Add operation for this command
     * @return Message
     */
    public CommandResponse execute(ModelService service) {
        //Retrieve domain to delete it
        CytomineDomain oldDomain = domain;
        //Init command info
        CytomineDomain container = oldDomain.container();
        if(container!=null && container instanceof Project && linkProject) {
            super.setProject((Project)container);
        }
        CommandResponse response = service.destroy(oldDomain, printMessage);
        fillCommandInfoJSON(backup.toString(), response.getData().get("message").toString());
        return response;
    }
}
