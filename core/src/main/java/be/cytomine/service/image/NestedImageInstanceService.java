package be.cytomine.service.image;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.NestedImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.repository.image.NestedImageInstanceRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.DateUtils;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class NestedImageInstanceService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private NestedImageInstanceRepository nestedImageInstanceRepository;


    @Override
    public Class currentDomain() {
        return NestedImageInstance.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new NestedImageInstance().buildDomainFromJson(json, getEntityManager());
    }

    public Optional<NestedImageInstance> find(Long id) {
        Optional<NestedImageInstance> nestedImageInstance = nestedImageInstanceRepository.findById(id);
        nestedImageInstance.ifPresent(cf -> {
            securityACLService.check(cf.container(),READ);
        });
        return nestedImageInstance;
    }

    public NestedImageInstance get(Long id) {
        return find(id).orElse(null);
    }

    public List<NestedImageInstance> list(ImageInstance image) {
        securityACLService.check(image.container(),READ);
        return nestedImageInstanceRepository.findAllByParent(image).stream().sorted(
                DateUtils.descCreatedComparator()
        ).collect(Collectors.toList());
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(json.getJSONAttrLong("project"), Project.class, READ);
        securityACLService.checkIsNotReadOnly(json.getJSONAttrLong("project"), Project.class);
        synchronized (this.getClass()) {
            return executeCommand(new AddCommand(currentUser),null, json);
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
        securityACLService.check(domain.container(),READ);
        securityACLService.checkUser(currentUser);
        securityACLService.check(jsonNewData.getJSONAttrLong("project"),Project.class,READ);
        securityACLService.checkIsNotReadOnly(domain.container());
        securityACLService.checkIsNotReadOnly(jsonNewData.getJSONAttrLong("project"),Project.class);
        return executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
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
        securityACLService.check(domain.container(),READ);
        securityACLService.checkUser(currentUser);
        securityACLService.checkIsNotReadOnly(domain.container());
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {
        NestedImageInstance nestedImageInstance = (NestedImageInstance)domain;
        Optional<NestedImageInstance> imageAlreadyExist = nestedImageInstanceRepository.findByBaseImageAndParentAndProject(nestedImageInstance.getBaseImage(), nestedImageInstance.getParent(), nestedImageInstance.getProject());
        if (imageAlreadyExist.isPresent() && (imageAlreadyExist.get().getId() != nestedImageInstance.getId())) {
            throw new AlreadyExistException("Nested Image " + nestedImageInstance.getBaseImage().getOriginalFilename() + " already map with image " + nestedImageInstance.getParentId() + " in project " + nestedImageInstance.getProject().getName());
        }
    }


    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((NestedImageInstance)domain).getBaseImage().getOriginalFilename(), ((NestedImageInstance)domain).getProject().getName());
    }

}
