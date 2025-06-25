package be.cytomine.service.meta;

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
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.meta.AttachedFileRepository;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
public class AttachedFileService extends ModelService {

    @Autowired
    private AttachedFileRepository attachedFileRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AnnotationDomainRepository annotationDomainRepository;

    @Override
    public Class currentDomain() {
        return AttachedFile.class;
    }

    public List<AttachedFile> list() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return attachedFileRepository.findAll();
    }

    public List<AttachedFile> findAllByDomain(CytomineDomain domain) {
        return findAllByDomain(domain.getClass().getName(), domain.getId());
    }

    public List<AttachedFile> findAllByDomain(String domainClassName, Long domainIdent) {
        if(domainClassName.contains("AnnotationDomain")) {
            AnnotationDomain annotation = annotationDomainRepository.findById(domainIdent)
                    .orElseThrow(() -> new ObjectNotFoundException(domainClassName, domainIdent));
            securityACLService.check(annotation, READ);
        } else {
            securityACLService.check(domainIdent,domainClassName, READ);
        }
        return attachedFileRepository.findAllByDomainClassNameAndDomainIdent(domainClassName, domainIdent);
    }

    public Optional<AttachedFile> findById(Long id) {
        Optional<AttachedFile> attachedFile = attachedFileRepository.findById(id);
        attachedFile.ifPresent(file -> securityACLService.check(file.getDomainIdent(),file.getDomainClassName(),READ));
        return attachedFile;
    }

    public AttachedFile create(String filename,byte[] data, String key, Long domainIdent,String domainClassName) throws ClassNotFoundException {
        User currentUser = currentUserService.getCurrentUser();
        CytomineDomain recipientDomain = getCytomineDomain(domainClassName, domainIdent);

        if (recipientDomain instanceof AbstractImage) {
            securityACLService.checkUser(currentUser);
            securityACLService.check(domainIdent,domainClassName,READ);
        } else{ securityACLService.checkUserAccessRightsForMeta( recipientDomain,  currentUser);}

        AttachedFile file = new AttachedFile();
        file.setDomainIdent(domainIdent);
        file.setDomainClassName(domainClassName);
        file.setFilename(filename);
        file.setData(data);
        file.setKey(key);
        saveDomain(file);
        return file;
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        AttachedFile attachedFile = (AttachedFile)domain;
        CytomineDomain parentDomain = getCytomineDomain(attachedFile.getDomainClassName(), attachedFile.getDomainIdent());

        if (parentDomain == null) {
            throw new ObjectNotFoundException(attachedFile.getDomainClassName(), attachedFile.getDomainIdent());
        }

        if (parentDomain instanceof AbstractImage) {
            securityACLService.checkUser(currentUser);
            securityACLService.check(attachedFile.getDomainIdent(),attachedFile.getDomainClassName(),READ);
        } else{ securityACLService.checkUserAccessRightsForMeta( parentDomain,  currentUser);}

        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }


    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return null;
    }


    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((AttachedFile)domain).getDomainClassName());
    }
}
