package be.cytomine.service.image.server;

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
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.User;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static org.springframework.security.acls.domain.BasePermission.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService extends ModelService {

    private final SecurityACLService securityACLService;

    private final ApplicationContext applicationContext;

    private final StorageRepository storageRepository;

    private final CurrentUserService currentUserService;

    private final CurrentRoleService currentRoleService;

    private final PermissionService permissionService;

    public List<Storage> list() {
        return securityACLService.getStorageList(currentUserService.getCurrentUser(), true);
    }

    public List<Storage> list(User user, String searchString) {
        return securityACLService.getStorageList(user, false, searchString);
    }

    public Optional<Storage> find(Long id) {
        Optional<Storage> Storage = storageRepository.findById(id);
        Storage.ifPresent(image -> securityACLService.check(image.container(),READ));
        return Storage;
    }

    public Storage get(Long id) {
        return find(id).orElse(null);
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        json.put("user", currentRoleService.isAdminByNow(currentUser) ? json.get("user") : currentUser.getId());
        return executeCommand(new AddCommand(currentUser),null, json);

    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        log.info("Add permission on {} to {}", domain, ((Storage)domain).getUser().getUsername());
        Storage storage = (Storage)domain;
        if(!permissionService.hasACLPermission(storage, READ)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), READ);
        }
        if(!permissionService.hasACLPermission(storage, WRITE)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), WRITE);
        }
        if(!permissionService.hasACLPermission(storage, ADMINISTRATION)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), ADMINISTRATION);
        }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(), ADMINISTRATION);
        CommandResponse commandResponse = executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
        return commandResponse;
    }


    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(), ADMINISTRATION);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    public void initUserStorage(final User user) {
        log.info("Initialize storage for {}", user.getUsername());

        Storage storage = new Storage();
        storage.setUser((User) user);
        storage.setName(user.getUsername() + " storage");
        storage = storageRepository.save(storage);

        String username = user.getUsername();
        if(!permissionService.hasACLPermission(storage, username, READ)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), READ, user);
        }
        if(!permissionService.hasACLPermission(storage, username, WRITE)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), WRITE, user);
        }
        if(!permissionService.hasACLPermission(storage, username, ADMINISTRATION)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), ADMINISTRATION, user);
        }
    }


    @Override
    public Class currentDomain() {
        return Storage.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Storage().buildDomainFromJson(json, getEntityManager());
    }



    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((Storage)domain).getName());
    }
}
