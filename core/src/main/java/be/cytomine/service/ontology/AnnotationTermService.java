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
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.AnnotationTermRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.command.TransactionService;
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

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class AnnotationTermService extends ModelService {

    @Autowired
    private AnnotationTermRepository annotationTermRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private UserAnnotationRepository userAnnotationRepository;

    @Autowired
    private TransactionService transactionService;

    @Override
    public Class currentDomain() {
        return AnnotationTerm.class;
    }


    public Optional<AnnotationTerm> find(AnnotationDomain annotation, Term term, User user) {
        securityACLService.check(annotation.container(),READ);
        List<AnnotationTerm> annotationTerms = annotationTermRepository.findAllByUserAnnotationId(annotation.getId());
        return annotationTerms.stream().filter(x -> x.getTerm()==term && (user==null || x.getUser().getId().equals(user.getId()))).findFirst();
    }

    public List<AnnotationTerm> list(UserAnnotation userAnnotation) {
        securityACLService.check(userAnnotation.container(),READ);
        return annotationTermRepository.findAllByUserAnnotation(userAnnotation);
    }


    public List<AnnotationTerm> list(UserAnnotation annotation, User user) {
        securityACLService.check(annotation.container(),READ);
        return annotationTermRepository.findAllByUserAndUserAnnotation(user, annotation);
    }

    public List<AnnotationTerm> list(Project project) {
        securityACLService.check(project.container(),READ);
        return annotationTermRepository.findAllByUserAnnotation_Project(project);
    }

    public List<AnnotationTerm> listAnnotationTermNotDefinedByUser(UserAnnotation userAnnotation, User user) {
        securityACLService.check(userAnnotation.container(),READ);
        return list(userAnnotation).stream().filter(x -> !x.getUser().equals(user)).collect(Collectors.toList());
    }


    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    public CommandResponse add(JsonObject jsonObject) {
        User currentUser = currentUserService.getCurrentUser();
        //Check if user has a role that allows to associate terms with annotations
        securityACLService.checkGuest(currentUser);
        User creator = userRepository.findById(jsonObject.getJSONAttrLong("user", -1L))
                .orElse(currentUser);
        jsonObject.put("user", creator.getId());

        UserAnnotation ua = userAnnotationRepository.findById(
                jsonObject.getJSONAttrLong("userannotation", -1L)
        ).orElseThrow(() -> new ObjectNotFoundException("UserAnnotation", jsonObject.getJSONAttrStr("userannotation")));
        //Check if user has at least READ permission for the project
        securityACLService.check(ua.container(), READ, currentUser);
        return executeCommand(new AddCommand(currentUser),null,jsonObject);
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
        //Check if user has a role that allows to associate terms with annotations
        securityACLService.checkGuest(currentUser);
        //if term is added from a user, check if the user has permission for UserAnnotation domain
        securityACLService.check(((AnnotationTerm)domain).getUserAnnotation().getId(), UserAnnotation.class, READ);
        //Check if user is admin, the project mode and if is the owner of the annotation
        securityACLService.checkFullOrRestrictedForOwner(domain, ((AnnotationTerm)domain).getUserAnnotation().getUser());
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    public CommandResponse addAnnotationTerm(Long idUserAnnotation, Long idTerm, Long idExpectedTerm, Long idUser, User currentUser, Transaction transaction) {
        Term term = termRepository.findById(idTerm)
                .orElseThrow(() -> new ObjectNotFoundException("Term", idExpectedTerm));
        UserAnnotation userAnnotation = userAnnotationRepository.findById(idUserAnnotation)
                .orElseThrow(() -> new ObjectNotFoundException("UserAnnotation", idUserAnnotation));
        User creator = userRepository.findById(idUser)
                .orElseThrow(() -> new ObjectNotFoundException("User", idUser));
        securityACLService.check(userAnnotation.container(),READ);
        JsonObject jsonObject = JsonObject.of(
                "userannotation", idUserAnnotation,
                "term", idTerm,
                "expectedTerm", idExpectedTerm,
                "user", idUser
        );
        return executeCommand(new AddCommand(currentUser, transaction),null,jsonObject);
    }

    public CommandResponse addWithDeletingOldTerm(Long idUserAnnotation, Long idTerm) {
        return addWithDeletingOldTerm(idUserAnnotation, idTerm, false);
    }

    /**
     * Add annotation-term for an annotation and delete all annotation-term that where already map with this annotation by this user
     */
    public CommandResponse addWithDeletingOldTerm(Long idAnnotation, Long idTerm, Boolean fromAllUser) {
        User currentUser = currentUserService.getCurrentUser();
        AnnotationDomain annotation = AnnotationDomain.findAnnotationDomain(getEntityManager(), idAnnotation)
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", idAnnotation));
        securityACLService.check(annotation.container(),READ,currentUser);
        if (annotation instanceof UserAnnotation) {
            Transaction transaction = transactionService.start();

            //Delete all annotation term
            List<AnnotationTerm> annotationTerms = annotationTermRepository.findAllByUserAnnotationId(annotation.getId())
                    .stream().filter(x -> fromAllUser || x.getUser().equals(currentUser)).collect(Collectors.toList());

            log.info("Delete old annotationTerm= " + annotationTerms.size());
            for (AnnotationTerm annotationTerm : annotationTerms) {
                log.info("unlink annotationTerm:" + annotationTerm.getId());
                this.delete(annotationTerm,transaction,null,true);
            }
            //Add annotation term
            return addAnnotationTerm(idAnnotation, idTerm, null, currentUser.getId(), currentUser, transaction);
        }  else if(annotation instanceof ReviewedAnnotation){
            Transaction transaction = transactionService.start();
            ReviewedAnnotation reviewed = (ReviewedAnnotation)annotation;
            reviewed.getTerms().clear();
            reviewed.getTerms().add(termRepository.getById(idTerm));
            getEntityManager().persist(reviewed);
            CommandResponse commandResponse = new CommandResponse();
            commandResponse.setStatus(200);
            return commandResponse;
        }
        throw new WrongArgumentException("annotation " + annotation.getClass() + " not supported");
    }





    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new AnnotationTerm().buildDomainFromJson(json, getEntityManager());
    }


    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        AnnotationTerm rt = (AnnotationTerm)domain;
        return Arrays.asList(String.valueOf(rt.getId()), String.valueOf(rt.getUserAnnotation().getId()), rt.getTerm().getName(), (rt.getUser()!=null?rt.getUser().getUsername():"undefined"));
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        throw new RuntimeException("Update is not implemented for Annotation Term");
    }


    public void checkDoNotAlreadyExist(CytomineDomain domain){
        AnnotationTerm annotationTerm = (AnnotationTerm)domain;
        if(annotationTerm!=null && annotationTerm.getUserAnnotation()!=null&& annotationTerm.getTerm()!=null&& annotationTerm.getUser()!=null) {
            Optional<AnnotationTerm> atAlreadyExist = annotationTermRepository.findByUserAnnotationAndTermAndUser(annotationTerm.getUserAnnotation(), annotationTerm.getTerm(), annotationTerm.getUser());
            if(atAlreadyExist.isPresent() && !Objects.equals(atAlreadyExist.get().getId(), domain.getId()))  {
                throw new AlreadyExistException("AnnotationTerm with annotation=" + annotationTerm.getUserAnnotation().getId() + " and term " + annotationTerm.getTerm().getId() + " and user " + annotationTerm.getUser() + " already exist!");
            }
        }
    }


    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        return;
    }


    /**
     * Retrieve domain thanks to a JSON object
     * @param json JSON with new domain info
     * @return domain retrieve thanks to json
     */
    @Override
    public CytomineDomain retrieve(JsonObject json) {
        return annotationTermRepository.findByUserAnnotationIdAndTermIdAndUserId(json.getJSONAttrLong("userannotation"),json.getJSONAttrLong("term"),json.getJSONAttrLong("user"))
                .orElseThrow(() -> new ObjectNotFoundException("Annotation-term not found " + json));
    }
}
