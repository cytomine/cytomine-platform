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
import be.cytomine.domain.meta.Tag;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.repository.meta.TagDomainAssociationRepository;
import be.cytomine.repository.meta.TagRepository;
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
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class TagService extends ModelService {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TagDomainAssociationRepository tagDomainAssocitationRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private TagDomainAssociationService tagDomainAssociationService;

    @Override
    public Class currentDomain() {
        return Tag.class;
    }

    public List<Tag> list() {
        securityACLService.checkGuest();
        return tagRepository.findAll();
    }

    public Tag get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<Tag> find(Long id) {
        return tagRepository.findById(id);
    }

    public Optional<Tag> findByName(String name) {
        return tagRepository.findByNameIgnoreCase(name);
    }

    @Override
    public CommandResponse add(JsonObject jsonObject) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        jsonObject.put("user", currentUser.getId());
        return executeCommand(new AddCommand(currentUser),null,jsonObject);
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkIsCreator(domain, currentUser);
        if (tagDomainAssocitationRepository.countByTag((Tag)domain) > 0) {
            //if not admin then check if there is no association
            securityACLService.checkAdmin(currentUser);
        }
        return executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkIsCreator(domain, currentUser);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }



    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Tag().buildDomainFromJson(json, getEntityManager());
    }

    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((Tag)domain).getName());
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain){
        Tag tag = (Tag)domain;
        if(tag!=null && tag.getName()!=null) {
            if(tagRepository.findByNameIgnoreCase(tag.getName()).stream().anyMatch(x -> !Objects.equals(x.getId(), tag.getId())))  {
                throw new AlreadyExistException("Tag " + tag.getName() + " already exist!");
            }
        }
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentTagDomainAssociation((Tag) domain, transaction, task);
    }

    private void deleteDependentTagDomainAssociation(Tag tag, Transaction transaction, Task task) {
        for (TagDomainAssociation tagDomainAssociation : tagDomainAssocitationRepository.findAllByTag(tag)) {
            tagDomainAssociationService.delete(tagDomainAssociation, transaction, task,false);
        }
    }
}
