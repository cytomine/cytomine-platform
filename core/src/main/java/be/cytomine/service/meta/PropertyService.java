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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import org.locationtech.jts.geom.Geometry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.utils.SQLUtils.castToLong;
import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class PropertyService extends ModelService {

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AnnotationDomainRepository annotationDomainRepository;

    @Override
    public Class currentDomain() {
        return Property.class;
    }


    public List<Property> list() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return propertyRepository.findAll();
    }

    public List<Property> list(CytomineDomain cytomineDomain) {
        securityACLService.check(cytomineDomain.container(),READ);
        return propertyRepository.findAllByDomainIdent(cytomineDomain.getId());
    }

    public Optional<Property> findById(Long id) {

        Optional<Property> property = propertyRepository.findById(id);
        if (property.isPresent()) {
            securityACLService.check(property.get().container(),READ);
        }
        return property;
    }

    public Optional<Property> findByDomainAndKey(CytomineDomain domain, String key) {
        Optional<Property> property = propertyRepository.findByDomainIdentAndKey(domain.getId(), key);
        if (property.isPresent()) {
            securityACLService.check(property.get().container(),READ);
        }
        return property;
    }

    @Override
    public CommandResponse add(JsonObject jsonObject) {
        return add(jsonObject, null, null);
    }

    public CommandResponse add(JsonObject jsonObject, Transaction transaction, Task task) {
        User currentUser = currentUserService.getCurrentUser();
        CytomineDomain domain = getCytomineDomain(jsonObject.getJSONAttrStr("domainClassName"), jsonObject.getJSONAttrLong("domainIdent"));
        if(!domain.getClass().getName().contains("AbstractImage")) {
            securityACLService.checkUserAccessRightsForMeta( domain,  currentUser);
        }else{
            //TODO when is this used ?
            securityACLService.checkUser(currentUser);
        }
        Command command = new AddCommand(currentUser,transaction);
        return executeCommand(command,null, jsonObject);
    }

    public CommandResponse addProperty(String domainClassName, Long domainIdent, String key, String value, User user, Transaction transaction) {
        JsonObject jsonObject = JsonObject.of(
                "domainClassName", domainClassName,
                "domainIdent", domainIdent,
                "key", key,
                "value", value
        );
        return executeCommand(new AddCommand(user, transaction), null, jsonObject);
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        User currentUser = currentUserService.getCurrentUser();
        CytomineDomain parentDomain = getCytomineDomain(((Property) domain).getDomainClassName(), ((Property) domain).getDomainIdent());
        if(!parentDomain.getClass().getName().contains("AbstractImage")) {
            securityACLService.checkUserAccessRightsForMeta(parentDomain, currentUser);
        }else{
            //TODO when is this used ?
            securityACLService.checkUser(currentUser);
        }
        return executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        CytomineDomain parentDomain = getCytomineDomain(((Property) domain).getDomainClassName(), ((Property) domain).getDomainIdent());
        if(!parentDomain.getClass().getName().contains("AbstractImage")) {
            securityACLService.checkUserAccessRightsForMeta(parentDomain, currentUser);
        }else{
            //TODO when is this used ?
            securityACLService.checkUser(currentUser);
        }
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Property().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        Property property = (Property)domain;
        return Arrays.asList(property.getKey(), property.getDomainClassName(), String.valueOf(property.getDomainIdent()));
    }

    public List<Map<String, Object>> listKeysForAnnotation(Project project, ImageInstance image, Boolean withUser) {
        if (project != null) {
            securityACLService.check(project,READ);
        } else {
            securityACLService.check(image.container(),READ);
        }

        String request = "SELECT DISTINCT p.key " +
                (withUser? ", ua.user_id " : "") +
                "FROM property as p, user_annotation as ua " +
                "WHERE p.domain_ident = ua.id " +
                (project!=null? "AND ua.project_id = '"+ project.getId() + "' " : "") +
                (image!=null? "AND ua.image_id = '"+ image.getId() + "' " : "") +
                "UNION " +
                "SELECT DISTINCT p2.key " +
                (withUser? ", ra.user_id " : "") +
                "FROM property as p2, reviewed_annotation as ra " +
                "WHERE p2.domain_ident = ra.id " +
                (project!=null? "AND ra.project_id = '"+ project.getId() + "' " : "") +
                (image!=null? "AND ra.image_id = '"+ image.getId() + "' " : "");

        return  selectListKeyWithUser(request, Map.of());
    }

    public List<String> listKeysForImageInstance(Project project) {
        if (project != null) {
            securityACLService.check(project,READ);
        }

        String request = "SELECT DISTINCT p.key " +
                "FROM property as p, image_instance as ii " +
                "WHERE p.domain_ident = ii.id " +
                "AND ii.project_id = "+ project.getId();

        return selectListkey(request, Map.of());
    }

    public List<Map<String, Object>> listAnnotationCenterPosition(User user, ImageInstance image, Geometry boundingbox, String key) {
        securityACLService.check(image.container(),READ);
        String request = "SELECT DISTINCT ua.id, ST_X(ST_CENTROID(ua.location)) as x,ST_Y(ST_CENTROID(ua.location)) as y, p.value " +
                "FROM user_annotation ua, property as p " +
                "WHERE p.domain_ident = ua.id " +
                "AND p.key = :key " +
                "AND ua.image_id = '"+ image.getId() +"' " +
                "AND ua.user_id = '"+ user.getId() +"' " +
                (boundingbox!=null ? "AND ST_Intersects(ua.location,ST_GeometryFromText('" + boundingbox.toString() + "',0)) " :"");

        return selectsql(request, Map.of("key", (Object)key));
    }

    private List<String> selectListkey(String request, Map<String, Object> parameters) {
        Query nativeQuery = getEntityManager().createNativeQuery(request, Tuple.class);
        for (Map.Entry<String, Object> map : parameters.entrySet()) {
            nativeQuery.setParameter(map.getKey(), map.getValue());
        }
        List<Tuple> resultList = nativeQuery.getResultList();
        return resultList.stream().map(x -> (String)x.get(0)).collect(Collectors.toList());
    }

    private List<Map<String, Object>> selectListKeyWithUser(String request, Map<String, Object> parameters) {
        Query nativeQuery = getEntityManager().createNativeQuery(request, Tuple.class);
        for (Map.Entry<String, Object> map : parameters.entrySet()) {
            nativeQuery.setParameter(map.getKey(), map.getValue());
        }
        List<Tuple> resultList = nativeQuery.getResultList();
        return resultList.stream().map(x -> Map.of("key", x.get(0), "user", (x.getElements().size()>1 ? castToLong(x.get(1)) : 0L))).collect(Collectors.toList());
    }

    private List<Map<String, Object>> selectsql(String request, Map<String, Object> parameters) {
        Query nativeQuery = getEntityManager().createNativeQuery(request, Tuple.class);
        for (Map.Entry<String, Object> map : parameters.entrySet()) {
            nativeQuery.setParameter(map.getKey(), map.getValue());
        }
        List<Tuple> resultList = nativeQuery.getResultList();
        return resultList.stream().map(x -> Map.of("idAnnotation", castToLong(x.get(0)), "x", x.get(1), "y", x.get(2), "value", x.get(3))).collect(Collectors.toList());
    }

}
