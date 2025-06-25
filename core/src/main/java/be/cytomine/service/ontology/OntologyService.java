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
import be.cytomine.domain.command.*;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ConstraintException;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
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
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
public class OntologyService extends ModelService {

    @Autowired
    private OntologyRepository ontologyRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TermService termService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;

    public Ontology get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<Ontology> find(Long id) {
        Optional<Ontology> optionalOntology = ontologyRepository.findById(id);
        optionalOntology.ifPresent(ontology -> securityACLService.check(ontology,READ));
        return optionalOntology;
    }

    /**
     * List ontology with full tree structure (term, relation,...)
     * Security check is done inside method
     */
    public List<Ontology> list() {
        return securityACLService.getOntologyList(currentUserService.getCurrentUser());
    }

    /**
     * List ontology with just id/name
     * Security check is done inside method
     */
    public List<JsonObject> listLight() {
        List<Ontology> ontologies = list();
        List<JsonObject>  data = new ArrayList<>();
        for (Ontology ontology : ontologies) {
            data.add(JsonObject.of("id", ontology.getId(), "name", ontology.getName()));
        }
        return data;
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
        securityACLService.check(domain,WRITE);
        securityACLService.checkUser(currentUser);
        return executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain,DELETE);
        securityACLService.checkUser(currentUser);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public Class currentDomain() {
        return Ontology.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Ontology().buildDomainFromJson(json, getEntityManager());
    }

    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((Ontology)domain).getName());
    }

    public void determineRightsForUsers(Ontology ontology, List<User> users) {
        for (User user : users) {
            determineRightsForUser(ontology, user);
        }
    }

    public void determineRightsForUser(Ontology ontology, User user) {
        List<Project> projects = projectRepository.findAllByOntology(ontology);
        if(projects.stream().anyMatch(project -> userService.listAdmins(project).contains(user))) {
            permissionService.addPermission(ontology, user.getUsername(), BasePermission.ADMINISTRATION);
        } else {
            if (ontology.getUser()!=user) {
                // if user is creator, he keep access to the ontology
                permissionService.deletePermission(ontology, user.getUsername(), BasePermission.ADMINISTRATION);
            }
        }
        if(projects.stream().anyMatch(project -> userService.listUsers(project).contains(user))) {
            permissionService.addPermission(ontology, user.getUsername(), BasePermission.READ);
        } else {
            if (ontology.getUser()!=user) {
                // if user is creator, he keep access to the ontology
                permissionService.deletePermission(ontology, user.getUsername(), BasePermission.READ);
            }
        }
    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        permissionService.addPermission(domain, currentUserService.getCurrentUsername(), BasePermission.ADMINISTRATION);
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain){
        Ontology ontology = (Ontology)domain;
        if(ontology!=null && ontology.getName()!=null) {
            if(ontologyRepository.findByName(ontology.getName()).stream().anyMatch(x -> !Objects.equals(x.getId(), ontology.getId())))  {
                throw new AlreadyExistException("Ontology " + ontology.getName() + " already exist!");
            }
        }
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentProject((Ontology) domain, transaction, task);
        deleteDependentTerm((Ontology) domain, transaction, task);
    }

    private void deleteDependentProject(Ontology ontology, Transaction transaction, Task task) {
        if(!projectRepository.findAllByOntology(ontology).isEmpty()) {
            throw new ConstraintException("Ontology is linked with project. Cannot delete ontology!");
        }
    }

    private void deleteDependentTerm(Ontology ontology, Transaction transaction, Task task) {
        for (Term term : termService.list(ontology)) {
            termService.delete(term, transaction, task,false);
        }
        ontology.setTerms(new HashSet<>()); //otherwise, when you write the json, term cannot be found (because term link is LAZY + term deleted)
    }



}
