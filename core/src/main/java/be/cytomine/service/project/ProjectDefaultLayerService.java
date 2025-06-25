package be.cytomine.service.project;

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
import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectDefaultLayer;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.project.ProjectDefaultLayerRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.security.UserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
public class ProjectDefaultLayerService extends ModelService {

    @Autowired
    private ProjectDefaultLayerRepository projectDefaultLayerRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;


    @Override
    public Class currentDomain() {
        return ProjectDefaultLayer.class;
    }

    public ProjectDefaultLayer get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<ProjectDefaultLayer> find(Long id) {
        Optional<ProjectDefaultLayer> optionalProjectDefaultLayer = projectDefaultLayerRepository.findById(id);
        optionalProjectDefaultLayer.ifPresent(projectDefaultLayer -> securityACLService.check(projectDefaultLayer,READ));
        return optionalProjectDefaultLayer;
    }

    /**
     * Get all default layers of the current project
     * @return ProjectDefaultLayer list
     */
    public List<ProjectDefaultLayer> listByProject(Project project) {
        securityACLService.check(project,READ);
        return projectDefaultLayerRepository.findAllByProject(project);
    }


    @Override
    public CommandResponse add(JsonObject jsonObject) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.check(jsonObject.getJSONAttrLong("project"),Project.class,WRITE);
        User user = userService.findUser(jsonObject.getJSONAttrLong("user"))
                .orElseThrow(() -> new ObjectNotFoundException("User", jsonObject.getJSONAttrStr("user")));
        Project project = projectRepository.findById(jsonObject.getJSONAttrLong("project"))
                .orElseThrow(() -> new ObjectNotFoundException("Project", jsonObject.getJSONAttrStr("project")));

        securityACLService.checkIsUserInProject(user, project);

        return executeCommand(new AddCommand(currentUser),null,jsonObject);
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        securityACLService.check(domain,WRITE);
        User currentUser = currentUserService.getCurrentUser();
        return executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(),WRITE);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }



    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new ProjectDefaultLayer().buildDomainFromJson(json, getEntityManager());
    }

    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        User user = ((ProjectDefaultLayer)domain).getUser();
        return List.of(domain.getId(), user.getFullName());
    }


    public void checkDoNotAlreadyExist(CytomineDomain domain){
        ProjectDefaultLayer projectDefaultLayer = (ProjectDefaultLayer)domain;
        if(projectDefaultLayer!=null) {
            if(projectDefaultLayerRepository.findByProjectAndUser(projectDefaultLayer.getProject(), projectDefaultLayer.getUser()).stream().anyMatch(x -> !Objects.equals(x.getId(), projectDefaultLayer.getId())))  {
                throw new AlreadyExistException("User "+projectDefaultLayer.getUser().getId()+" has already default layer of the project " + projectDefaultLayer.getProject().getId());
            }
        }
    }

}
