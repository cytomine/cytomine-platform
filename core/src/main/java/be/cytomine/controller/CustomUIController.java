package be.cytomine.controller;

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

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.ServerException;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.meta.PropertyService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Lock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@RestController
@RequestMapping("")
@Slf4j
@RequiredArgsConstructor
public class CustomUIController extends RestCytomineController {

    private final CurrentRoleService currentRoleService;

    private final CurrentUserService currentUserService;

    private final ProjectService projectService;

    private final SecurityACLService securityACLService;

    private final PropertyService propertyService;

    private final ApplicationProperties applicationProperties;

    static String CUSTOM_UI_PROJECT = "@CUSTOM_UI_PROJECT";

    @GetMapping({"/api/custom-ui/config.json", "/custom-ui/config.json"})
    public ResponseEntity<String> retrieveUIConfig(
            @RequestParam(required = false, value = "project", defaultValue = "0") Long projectId
    ) {
        log.debug("REST request to retrieve custom UI");
        Set<SecRole> roles = currentRoleService.findCurrentRole(currentUserService.getCurrentUser());
        Project project = projectService.get(projectId);

        JsonObject config = new JsonObject();
        config.putAll(getGlobalConfig(roles));
        if(project!=null) {
            config.putAll(getProjectConfigCurrentUser(project));
        }
        return responseSuccess(config);
    }


    @GetMapping({"/api/custom-ui/project/{project}.json", "/custom-ui/project/{project}.json"}) // DEPRECATED
    public ResponseEntity<String> showCustomUIForProject(
            @PathVariable(value = "project") Long projectId
    ) {
        log.debug("REST request to retrieve custom UI for project");
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        return responseSuccess(JsonObject.toJsonString(getProjectConfig(project)));
    }


    @PostMapping({"/api/custom-ui/project/{project}.json", "/custom-ui/project/{project}.json"}) // DEPRECATED
    public ResponseEntity<String> addCustomUIForProject(
            @PathVariable(value = "project") Long projectId,
            @RequestBody JsonObject jsonObject
    ) {
        log.debug("REST request to save custom UI for project");
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        if (Lock.getInstance().lockCustomUI(project)) {
            try {
                Optional<Property> optionalProperty = propertyService.findByDomainAndKey(project,CUSTOM_UI_PROJECT);
                securityACLService.check(project,ADMINISTRATION);

                if(optionalProperty.isEmpty()) {
                    Property property = new Property();
                    property.setKey(CUSTOM_UI_PROJECT);
                    property.setValue(jsonObject.toJsonString());
                    property.setDomain(project);

                    CommandResponse result = propertyService.add(property.toJsonObject());
                    responseSuccess((String)((LinkedHashMap)result.getData().get("property")).get("value"));
                } else {
                    JsonObject jsonEdit = optionalProperty.get().toJsonObject()
                            .withChange("value", jsonObject.toJsonString());

                    CommandResponse result = propertyService.update(optionalProperty.get(),jsonEdit);
                    responseSuccess((String)((LinkedHashMap)result.getData().get("property")).get("value"));
                }

                return responseSuccess(JsonObject.toJsonString(getProjectConfig(project)));
            } finally {
                Lock.getInstance().unlockCustomUI(project);
            }
        } else {
            throw new ServerException("Cannot acquire lock for custom UI project " + project.getId()  + " , tryLock return false");
        }
    }


    public JsonObject getGlobalConfig(Set<SecRole> roles) {
        JsonObject globalConfig = new JsonObject();
        Set<String> authorities = roles.stream().map(x -> x.getAuthority()).collect(Collectors.toSet());

        for (Map.Entry<String, List<String>> it : applicationProperties.getCustomUI().getGlobal().entrySet()) {
            boolean print;
            List<String> mandatoryRoles = it.getValue();
            if (mandatoryRoles.contains("ALL")) {
                print = true;
            } else {
                print = mandatoryRoles.stream().anyMatch(authorities::contains);
            }
            globalConfig.put(it.getKey(), print);
        }
        return globalConfig;
    }


    public Map<String, Map<String, Boolean>> getProjectConfig(Project project) {
        // clone config so that the default configuration is not updated
        Map<String, Map<String, Boolean>> result = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, Boolean>> it : applicationProperties.getCustomUI().getProject().entrySet()) {
            LinkedHashMap<String, Boolean> value = new LinkedHashMap<>();
            for (Map.Entry<String, Boolean> itValue : it.getValue().entrySet()) {
                value.put(itValue.getKey(), Boolean.valueOf(itValue.getValue().toString()));
            }
            result.put(it.getKey(), value);
        }

        Optional<Property> optionalProperty = propertyService.findByDomainAndKey(project,CUSTOM_UI_PROJECT);
        // if a property is saved, we override the default config
        if(optionalProperty.isPresent()) {
            JsonObject configProject = JsonObject.toJsonObject(optionalProperty.get().getValue());

            for (Map.Entry<String, Object> it : configProject.entrySet()) {
                result.put(it.getKey(), (Map<String, Boolean>)it.getValue());
            }
        }

        return result;
    }


    private Map<String, Boolean> getProjectConfigCurrentUser(Project project) {
        boolean isProjectAdmin = projectService.listByAdmin((User) currentUserService.getCurrentUser()).stream().anyMatch(x -> x.getId().equals(project.getId()));
        boolean isAdminByNow = currentRoleService.isAdminByNow(currentUserService.getCurrentUser());

        Map<String, Map<String, Boolean>> configProject = getProjectConfig(project);
        Map<String, Boolean> result = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Boolean>> it : configProject.entrySet()) {
            result.put(it.getKey(), shouldBeShown(isAdminByNow, isProjectAdmin, it.getValue()));
        }
        return result;
    }

    private boolean shouldBeShown(boolean isAdminByNow, boolean isProjectAdmin, Map<String, Boolean> config) {
        if(isAdminByNow) {
            return true;
        }

        if(isProjectAdmin) {
            return config.get("ADMIN_PROJECT");
        }

        return config.get("CONTRIBUTOR_PROJECT");
    }


}
