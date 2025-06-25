package be.cytomine.service.ontology;

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
import be.cytomine.domain.command.AddCommand;
import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.dto.image.CropParameter;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.SharedAnnotation;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.repository.ontology.SharedAnnotationRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class SharedAnnotationService extends ModelService {

    @Autowired
    private SharedAnnotationRepository sharedAnnotationRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AnnotationDomainRepository annotationDomainRepository;

    @Autowired
    private CurrentRoleService currentRoleService;

    @Override
    public Class currentDomain() {
        return SharedAnnotation.class;
    }

    /**
     * List all sharedAnnotation, Only for admin
     */
    public List<SharedAnnotation> list() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return sharedAnnotationRepository.findAll();
    }

    public SharedAnnotation get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<SharedAnnotation> find(Long id) {
        Optional<SharedAnnotation> optionalSharedAnnotation = sharedAnnotationRepository.findById(id);
        optionalSharedAnnotation.ifPresent(sharedAnnotation -> securityACLService.check(sharedAnnotation.container(),READ));
        return optionalSharedAnnotation;
    }


    public List<SharedAnnotation> listComments(AnnotationDomain annotation) {
        User user = (User)currentUserService.getCurrentUser();
        List<SharedAnnotation> sharedAnnotations = sharedAnnotationRepository
                .findAllByAnnotationIdentOrderByCreatedDesc(annotation.getId());
        boolean isUserAdmin = currentRoleService.isAdminByNow(user);
        sharedAnnotations = sharedAnnotations.stream().filter(x -> isUserAdmin || x.getSender().equals(user) || x.getReceivers().contains(user))
                .distinct()
                .collect(Collectors.toList());

        return sharedAnnotations;
    }


    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    @Transactional(dontRollbackOn = IOException.class)
    public CommandResponse add(JsonObject jsonObject) {
        User sender = (User)currentUserService.getCurrentUser();
        securityACLService.checkUser(sender);

        AnnotationDomain annotation = annotationDomainRepository.findById(jsonObject.getJSONAttrLong("annotationIdent"))
                        .orElseThrow(() -> new ObjectNotFoundException("Annotation", jsonObject.getJSONAttrStr("annotationIdent")));

        jsonObject.putIfAbsent("sender", sender.getId());

        securityACLService.checkFullOrRestrictedForOwner(annotation, annotation.user());
        return executeCommand(new AddCommand(sender), null, jsonObject);
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
        securityACLService.checkFullOrRestrictedForOwner(domain.container(),((SharedAnnotation)domain).getSender());
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }


    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new SharedAnnotation().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        SharedAnnotation sharedAnnotation = (SharedAnnotation)domain;
        return Arrays.asList(String.valueOf(sharedAnnotation.getSender().getId()), String.valueOf(sharedAnnotation.getAnnotationIdent()), sharedAnnotation.getAnnotationClassName());
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        throw new RuntimeException("Update is not implemented for shared annotation");
    }

    @Override
    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
    }

}
