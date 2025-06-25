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
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ConstraintException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.*;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
public class TermService extends ModelService {

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private RelationTermService relationTermService;

    @Autowired
    private AnnotationTermRepository annotationTermRepository;

    @Autowired
    private ReviewedAnnotationRepository reviewedAnnotationRepository;

    @Override
    public Class currentDomain() {
        return Term.class;
    }

    /**
     * List all term, Only for admin
     */
    public List<Term> list() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return termRepository.findAll();
    }

    public Term get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<Term> find(Long id) {
        Optional<Term> optionalTerm = termRepository.findById(id);
        optionalTerm.ifPresent(term -> securityACLService.check(term.container(),READ));
        return optionalTerm;
    }

    public List<Term> list(Ontology ontology) {
        securityACLService.check(ontology.container(),READ);
        return termRepository.findAllByOntology(ontology);
    }

    public List<Term> list(Project project) {
        securityACLService.check(project,READ);
        return termRepository.findAllByOntology(project.getOntology());
    }


    public List<Long> getAllTermId(Project project) {
        securityACLService.check(project.container(), READ);
        if (project.getOntology() == null) {
            return List.of();
        } else {
            return termRepository.listAllIds(project.getOntology());
        }
    }

    public String fillEmptyTermIds(String terms, Project project){
        if (terms == null || terms.equals("")) {
            return this.getAllTermId(project).stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return terms;
    }

    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    public CommandResponse add(JsonObject jsonObject) {
        if (jsonObject.isMissing("ontology")) {
            throw new WrongArgumentException("Ontology is mandatory for term creation");
        }
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkGuest(currentUser);
        securityACLService.check(jsonObject.getJSONAttrLong("ontology"), Ontology.class ,WRITE);
        return executeCommand(new AddCommand(currentUser),null,jsonObject);
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
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(),WRITE);
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
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(),DELETE);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }


    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Term().buildDomainFromJson(json, getEntityManager());
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain){
        Term term = (Term)domain;
        if(term!=null && term.getName()!=null) {
            if(termRepository.findByNameAndOntology(term.getName(), term.getOntology()).stream().anyMatch(x -> !Objects.equals(x.getId(), term.getId())))  {
                throw new AlreadyExistException("Term " + term.getName() + " already exist in this ontology!");
            }
        }
    }

    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        Term term = (Term)domain;
        return Arrays.asList(String.valueOf(term.getId()), term.getName(), term.getOntology().getName());
    }

    @Override
    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentRelationTerm((Term)domain, transaction, task);
        deleteAnnotationTerm((Term)domain, transaction, task);
        deleteReviewedAnnotationTerm((Term)domain, transaction, task);
    }

    public void deleteDependentRelationTerm(Term term, Transaction transaction, Task task) {
        for (RelationTerm relationTerm : relationTermService.list(term)) {
            relationTermService.delete(relationTerm, transaction, task, false);
        }
    }

    public void deleteAnnotationTerm(Term term, Transaction transaction, Task task) {
        long terms = annotationTermRepository.countByTerm(term);
        if (terms!=0) {
            throw new ConstraintException("Term is still linked with "+(terms)+" annotations created by user. Cannot delete term!");
        }
    }

    public void deleteReviewedAnnotationTerm(Term term, Transaction transaction, Task task) {
        long terms = reviewedAnnotationRepository.countAllByTermsContaining(term);
        if (terms!=0) {
            throw new ConstraintException("Term is still linked with "+(terms)+" reviewed annotations. Cannot delete term!");
        }
    }
}
