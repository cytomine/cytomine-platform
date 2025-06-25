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
import be.cytomine.domain.meta.Tag;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.meta.TagDomainAssociationRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import be.cytomine.utils.filters.SQLSearchParameter;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import be.cytomine.utils.filters.SpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
public class TagDomainAssociationService extends ModelService {

    @Autowired
    private TagDomainAssociationRepository tagDomainAssociationRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public Class currentDomain() {
        return TagDomainAssociation.class;
    }


    public TagDomainAssociation get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<TagDomainAssociation> find(Long id) {
        Optional<TagDomainAssociation> op = tagDomainAssociationRepository.findById(id);
        op.ifPresent(x -> {
            if (!x.getDomainClassName().contains("AbstractImage")) {
                securityACLService.check(x.container(),READ);
            }
        });
        return op;
    }

    public List<TagDomainAssociation> list(List<SearchParameterEntry> searchParameters) {
        List<SearchParameterEntry> validSearchParameters = SQLSearchParameter.getDomainAssociatedSearchParameters(TagDomainAssociation.class, searchParameters, getEntityManager());

        Specification<TagDomainAssociation> specification = SpecificationBuilder.getSpecificationFromFilters(validSearchParameters);

        List<TagDomainAssociation> associations = tagDomainAssociationRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "domainClassName"));

        List<TagDomainAssociation> results = new ArrayList<>();
        Map<Long, Boolean> domainIdsAlreadyCheck = new HashMap<>();

        for (TagDomainAssociation association : associations) {
            Boolean granted = domainIdsAlreadyCheck.get(association.getDomainIdent());
            // true = OK, false = Forbidden , null = not yet check
            if (granted==null) {
                // check authorization
                try {
                    if (!association.getDomainClassName().contains("AbstractImage")) {
                        try {
                            securityACLService.check(association.container(), READ);
                        } catch (ObjectNotFoundException exception) {
                            // parent does not exists in database, ignore
                            domainIdsAlreadyCheck.put(association.getDomainIdent(), false);
                            continue;
                        }
                    }
                    results.add(association);
                    domainIdsAlreadyCheck.put(association.getDomainIdent(), true);
                } catch (ForbiddenException ignored) {
                    domainIdsAlreadyCheck.put(association.getDomainIdent(), false);
                }
            } else if (granted) {
                results.add(association);
            }
        }

        return results;
    }

    public List<TagDomainAssociation> listAllByTag(Tag tag) {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return list(new ArrayList<>(List.of(new SearchParameterEntry("tag", SearchOperation.in, tag.getId()))));
    }

    public List<TagDomainAssociation> listAllByDomain(CytomineDomain domain) {
        if (!(domain instanceof AbstractImage)) {
            securityACLService.check(domain.container(),READ);
        }
        return list(new ArrayList<>(List.of(new SearchParameterEntry("domainClassName", SearchOperation.equals, domain.getClass().getName()),
                new SearchParameterEntry("domainIdent", SearchOperation.equals, domain.getId()))));
    }


    @Override
    public CommandResponse add(JsonObject jsonObject) {
        User currentUser = currentUserService.getCurrentUser();
        //Get the associated domain
        CytomineDomain domain = getCytomineDomain(jsonObject.getJSONAttrStr("domainClassName"), jsonObject.getJSONAttrLong("domainIdent"));
        if(!domain.getClass().getName().contains("AbstractImage")) {
            securityACLService.checkUserAccessRightsForMeta( domain,  currentUser);
        }else{
            //TODO when is this used ?
            securityACLService.checkUser(currentUser);
        }
        jsonObject.put("user", currentUser.getId());
        return executeCommand(new AddCommand(currentUser),null,jsonObject);
    }



    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        CytomineDomain parentDomain = getCytomineDomain(((TagDomainAssociation) domain).getDomainClassName(), ((TagDomainAssociation) domain).getDomainIdent());
        if(!parentDomain.getClass().getName().contains("AbstractImage")) {
            securityACLService.checkUserAccessRightsForMeta(parentDomain, currentUser);
        }else{
            //TODO when is this used ?
            securityACLService.checkUser(currentUser);
        }
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(((TagDomainAssociation)domain).getTag().getName(), ((TagDomainAssociation)domain).getDomainIdent(), ((TagDomainAssociation)domain).getDomainClassName());
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new TagDomainAssociation().buildDomainFromJson(json, getEntityManager());
    }


    public void checkDoNotAlreadyExist(CytomineDomain domain){
        TagDomainAssociation tagDomainAssociation = (TagDomainAssociation)domain;
        if(tagDomainAssociation!=null && tagDomainAssociation.getTag()!=null) {
            if(tagDomainAssociationRepository.findByTagAndDomainClassNameAndDomainIdent(tagDomainAssociation.getTag(), tagDomainAssociation.getDomainClassName(), tagDomainAssociation.getDomainIdent())
                    .stream().anyMatch(x -> !Objects.equals(x.getId(), tagDomainAssociation.getId())))  {
                throw new AlreadyExistException("Tag " + tagDomainAssociation.getTag().getName() + " already exist for " + tagDomainAssociation.getDomainClassName() + " " + tagDomainAssociation.getDomainIdent() + "!");
            }
        }
    }




}
