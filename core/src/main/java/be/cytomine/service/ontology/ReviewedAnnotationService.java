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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.ReviewedAnnotationStatsEntry;
import be.cytomine.dto.UserTermMapping;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ReviewedAnnotationListing;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.repository.ontology.ReviewedAnnotationRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.service.utils.ValidateGeometryService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class ReviewedAnnotationService extends ModelService {

    @Autowired
    private ReviewedAnnotationRepository reviewedAnnotationRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AnnotationListingService annotationListingService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ImageInstanceRepository imageInstanceRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ValidateGeometryService validateGeometryService;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserAnnotationRepository userAnnotationRepository;

    @Autowired
    private GenericAnnotationService genericAnnotationService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Class currentDomain() {
        return ReviewedAnnotation.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new ReviewedAnnotation().buildDomainFromJson(json, entityManager);
    }

    public ReviewedAnnotation get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<ReviewedAnnotation> find(Long id) {
        Optional<ReviewedAnnotation> optionalReviewedAnnotation = reviewedAnnotationRepository.findById(id);
        optionalReviewedAnnotation.ifPresent(reviewedAnnotation -> securityACLService.check(reviewedAnnotation.container(),READ));
        return optionalReviewedAnnotation;
    }

    public Long count(User user) {
        return reviewedAnnotationRepository.countByUser(user);
    }

    public Long countByProject(Project project, Date startDate, Date endDate) {
        if (startDate!=null && endDate!=null) {
            return reviewedAnnotationRepository.countByProjectAndCreatedBetween(project, startDate, endDate);
        } else if (startDate!=null) {
            return reviewedAnnotationRepository.countByProjectAndCreatedAfter(project, startDate);
        } else if (endDate!=null) {
            return reviewedAnnotationRepository.countByProjectAndCreatedBefore(project, endDate);
        } else {
            return reviewedAnnotationRepository.countByProject(project);
        }
    }

    public Long countByProjectAndWithTerms(Project project) {
        return reviewedAnnotationRepository.countAllByProjectAndTerms_Empty(project);
    }

    public List list(Project project, List<String> propertiesToShow) {
        securityACLService.check(project.container(), READ);
        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setColumnsToPrint(propertiesToShow);
        reviewedAnnotationListing.setProject(project.getId());
        return annotationListingService.executeRequest(reviewedAnnotationListing);
    }

    public Optional<ReviewedAnnotation> findByParent(AnnotationDomain annotationDomain) {
         return reviewedAnnotationRepository.findByParentIdent(annotationDomain.getId());
    }

    public List<ReviewedAnnotationStatsEntry> statsGroupByUser(ImageInstance ImageInstance) {
        return reviewedAnnotationRepository.stats(ImageInstance);
    }


    public List listIncluded(ImageInstance image, String geometry, List<Long> terms, AnnotationDomain annotation, List<String> propertiesToShow) {
        securityACLService.check(image.container(), READ);

        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setColumnsToPrint(propertiesToShow);
        reviewedAnnotationListing.setImage(image.getId());
        reviewedAnnotationListing.setTerms(terms);
        reviewedAnnotationListing.setExcludedAnnotation((annotation!=null? annotation.getId() : null));
        reviewedAnnotationListing.setBbox(geometry);
        return annotationListingService.executeRequest(reviewedAnnotationListing);
    }

    public List<UserTermMapping> listTerms(ReviewedAnnotation annotation) {
        Long reviewer = (annotation.getImage().getReviewUser()!=null?
                annotation.getImage().getReviewUser().getId() : null);
        return annotation.getTerms().stream().map(x -> new UserTermMapping(x.getId(), reviewer))
                .collect(Collectors.toList());
    }

    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    public CommandResponse add(JsonObject jsonObject) {
        securityACLService.check(jsonObject.getJSONAttrLong("project"), Project.class, READ);
        securityACLService.checkIsNotReadOnly(jsonObject.getJSONAttrLong("project"), Project.class);
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        //Start transaction
        Transaction transaction = transactionService.start();

        if (jsonObject.get("location") instanceof Geometry) {
            jsonObject.put("location", validateGeometryService.tryToMakeItValidIfNotValid((Geometry)jsonObject.get("location")));
        } else {
            jsonObject.put("location", validateGeometryService.tryToMakeItValidIfNotValid(jsonObject.getJSONAttrStr("location")));
        }

        synchronized (this.getClass()) {
            CommandResponse commandResponse = executeCommand(new AddCommand(currentUser, transaction), null, jsonObject);
            return commandResponse;
        }
    }


    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return Response structure (new domain data, old domain data..)
     */
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.checkIsCreator(domain, currentUser);
        CommandResponse result = executeCommand(new EditCommand(currentUser, null), domain, jsonNewData);
        return result;
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
        securityACLService.checkIsCreator(domain, currentUser);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }



    public CommandResponse reviewAnnotation(Long idAnnotation, List<Long> terms) {

        AnnotationDomain basedAnnotation = AnnotationDomain.getAnnotationDomain(entityManager, idAnnotation);
        if(!basedAnnotation.getImage().isInReviewMode()) {
            throw new WrongArgumentException("Cannot accept annotation, enable image review mode!");
        }
        if(basedAnnotation.getImage().getReviewUser()!=null
                && !Objects.equals(basedAnnotation.getImage().getReviewUser().getId(), currentUserService.getCurrentUser().getId())) {
            throw new WrongArgumentException("You must be the image reviewer to accept annotation. Image reviewer is " + basedAnnotation.getImage().getReviewUser());
        }

        reviewedAnnotationRepository.findByParentIdent(basedAnnotation.getId()).ifPresent(
                duplicate -> {throw new AlreadyExistException("Annotation is already accepted!");}
        );

        ReviewedAnnotation review = createReviewAnnotation(basedAnnotation, terms);
        return this.add(review.toJsonObject());
    }

    public CommandResponse unReviewAnnotation(Long idAnnotation) {
        ReviewedAnnotation reviewedAnnotation = reviewedAnnotationRepository.findByParentIdent(idAnnotation)
                .orElseThrow(() -> new WrongArgumentException("This annotation is not accepted, you cannot reject it!"));

        if(reviewedAnnotation.getImage().getReviewUser()!=null && !Objects.equals(reviewedAnnotation.getImage().getReviewUser().getId(), currentUserService.getCurrentUser().getId())) {
            throw new WrongArgumentException("You must be the image reviewer to reject annotation. Image reviewer is  " + reviewedAnnotation.getImage().getReviewUser().getUsername());
        }
        return this.delete(this.retrieve(JsonObject.of("id", reviewedAnnotation.getId())), null, null, false);
    }


    /**
     * Review annotation with the specified terms
     * @param annotation Annotation to review
     * @param terms Terms to add to the annotation
     * @return The reviewed annotation
     */
    private ReviewedAnnotation createReviewAnnotation(AnnotationDomain annotation, List<Long> terms) {
        ReviewedAnnotation review = new ReviewedAnnotation();
        review.setParentIdent(annotation.getId());
        review.setParentClassName(annotation.getClass().getName());
        review.setStatus(1);
        review.setUser(annotation.user());
        review.setLocation(annotation.getLocation());
        review.setImage(annotation.getImage());
        review.setSlice(annotation.getSlice());
        review.setProject(annotation.getProject());
        review.setGeometryCompression(annotation.getGeometryCompression());

        if(terms!=null) {
            //terms in request param
            for (Long term : terms) {
                review.getTerms().add(termRepository.findById(term)
                        .orElseThrow(() -> new ObjectNotFoundException("Term", term)));
            }
        } else {
            //nothing in param, add term from annotation
            for (Term term : annotation.termsForReview()) {
                review.getTerms().add(term);
            }

        }
        review.setReviewUser(currentUserService.getCurrentUser());
        return review;
    }



    public List<Long> reviewLayer(Long imageInstanceId, List<Long> usersIds, Task task) {

        taskService.updateTask(task, 2, "Extract parameters...");
        if (usersIds==null || usersIds.isEmpty()) {
            throw new WrongArgumentException("There is no layer:" + usersIds);
        }
        List<User> users = usersIds.stream()
                .map(x -> userRepository.findById(x).orElseThrow(() -> new ObjectNotFoundException("User", x))).collect(Collectors.toList());
        ImageInstance imageInstance = imageInstanceRepository.findById(imageInstanceId)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", imageInstanceId));

        if (!imageInstance.isInReviewMode()) {
            throw new WrongArgumentException("Cannot review annotation, enable image review mode!");
        } else if (imageInstance.getReviewUser() != null && !Objects.equals(imageInstance.getReviewUser().getId(), currentUserService.getCurrentUser().getId())) {
            throw new WrongArgumentException("You must be the image reviewer to review annotation. Image reviewer is " + imageInstance.getReviewUser());
        } else if (users.isEmpty()) {
            throw new WrongArgumentException("There is no layer:" + usersIds);
        }

        taskService.updateTask(task, 2, "Extract parameters...");

        List<AnnotationDomain> annotations = new ArrayList<>();

        //get all annotations for each user
        taskService.updateTask(task,5,"Look for all annotations...");

        for (User user : users) {
            annotations.addAll(userAnnotationRepository.findAllByUserAndImage((User)user, imageInstance));
        }

        //review each annotation
        taskService.updateTask(task,10,annotations.size() + " annotations found...");

        int realReviewed = 0;
        int taskRefresh =  annotations.size()>1000? 100 : 10;
        List<Long> reviewedIds = new ArrayList<>();
        for(int i = 0; i < annotations.size(); i++) {
            if (i%taskRefresh==0) {
                taskService.updateTask(task,10+(int)(((double)i/(double)annotations.size())*0.9d*100),realReviewed + " new reviewed annotations...");
            }
            entityManager.refresh(annotations.get(i));
            if (reviewedAnnotationRepository.findByParentIdent(annotations.get(i).getId()).isEmpty()) {
                //if not yet reviewed, review it
                realReviewed++;
                CommandResponse review = reviewAnnotation(annotations.get(i).getId(), null);
                reviewedIds.add(review.getObject().getId());
            }


        }
        taskService.finishTask(task);
        return reviewedIds;
    }

    public List<Long> unreviewLayer(Long imageInstanceId, List<Long> usersIds, Task task) {

        taskService.updateTask(task,2,"Extract parameters...");
        List<User> users = usersIds.stream()
                .map(x -> userRepository.findById(x).orElseThrow(() -> new ObjectNotFoundException("User", x))).collect(Collectors.toList());
        ImageInstance imageInstance = imageInstanceRepository.findById(imageInstanceId)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", imageInstanceId));

        //check constraint
        taskService.updateTask(task,3,"Review "+users.size()+" layers...");
        if(!imageInstance.isInReviewMode()) {
            throw new WrongArgumentException("Cannot reject annotation, enable image review mode!");
        } else if(imageInstance.getReviewUser()!=null && !Objects.equals(imageInstance.getReviewUser().getId(), currentUserService.getCurrentUser().getId())) {
            throw new WrongArgumentException("You must be the image reviewer to reject annotation. Image reviewer is " + imageInstance.getReviewUser().getUsername());
        } else if(users.isEmpty()) {
            throw new WrongArgumentException("There is no layer:"+usersIds);
        }

        List<AnnotationDomain> annotations = new ArrayList<>();
        taskService.updateTask(task,5,"Look for all annotations...");
        for (User user : users) {
            annotations.addAll(userAnnotationRepository.findAllByUserAndImage((User)user, imageInstance));
        }

        //unreview each one
        taskService.updateTask(task,10,annotations.size() + " annotations found...");
        int realUnReviewed = 0;
        int taskRefresh =  annotations.size()>1000? 100 : 10;

        List<ReviewedAnnotation> reviewed = new ArrayList<>();
        for(int i = 0; i < annotations.size(); i++) {
            if (i%taskRefresh==0) {
                taskService.updateTask(task,10+(int)(((double)i/(double)annotations.size())*0.9d*100),realUnReviewed + " new reviewed annotations...");
            }
            Optional<ReviewedAnnotation> optionalReviewedAnnotation = reviewedAnnotationRepository.findByParentIdent(annotations.get(i).getId());

            if (optionalReviewedAnnotation.isPresent()) {
                realUnReviewed++;
                reviewed.add(optionalReviewedAnnotation.get());
            }


        }

        if (!reviewed.isEmpty()) {
            reviewedAnnotationRepository.deleteAll(reviewed);
        }



        taskService.finishTask(task);
        return reviewed.stream().map(CytomineDomain::getId).collect(Collectors.toList());
    }

    protected void beforeAdd(CytomineDomain domain) {
        // this will be done in the PrePersist method ; but the validation is done before PrePersist
        ((ReviewedAnnotation)domain).setWktLocation(((ReviewedAnnotation)domain).getLocation().toText());
    }

    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        ReviewedAnnotation annotation = (ReviewedAnnotation)domain;
        return List.of(currentUserService.getCurrentUser().toString(), annotation.getImage().getBaseImage().getOriginalFilename());
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {
        // TODO: with new session?
        Optional<ReviewedAnnotation> annotationAlreadyExist = reviewedAnnotationRepository.findByParentIdent(((ReviewedAnnotation)domain).getParentIdent());
        if (annotationAlreadyExist.isPresent() && (!Objects.equals(annotationAlreadyExist.get().getId(), domain.getId()))) {
            throw new AlreadyExistException("Annotation " + ((ReviewedAnnotation)domain).getParentIdent() + " has already been reviewed");
        }
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        ((ReviewedAnnotation)domain).getTerms().clear();
    }

    /**
     * Apply a union or a diff on all covering annotations list with the newLocation geometry
     * @param coveringAnnotations List of reviewed annotations id that are covering by newLocation geometry
     * @param newLocation A geometry (wkt format)
     * @param remove Flag that tell to extend or substract part of geometry from  coveringAnnotations list
     * @return The first annotation data
     */
    // TODO: could be generic (same logic as for user annotation)
    public CommandResponse doCorrectReviewedAnnotation(List<Long> coveringAnnotations, String newLocation, boolean remove) throws ParseException {
        if (coveringAnnotations.isEmpty()) {
            throw new WrongArgumentException("Covering annotations are empty");
        }

        //Get the based annotation
        ReviewedAnnotation based = this.find(coveringAnnotations.get(0)).get();

        //Get the term of the based annotation, it will be the main term
        List<Long> basedTerms = based.termsId();

        //Get all other annotation with same term
        List<Long> allOtherAnnotationId = coveringAnnotations.subList(1, coveringAnnotations.size());
        List<ReviewedAnnotation> allAnnotationWithSameTerm = genericAnnotationService.findReviewedAnnotationWithTerm(allOtherAnnotationId, basedTerms);

        //Create the new geometry
        Geometry newGeometry = new WKTReader().read(newLocation);
        if(!newGeometry.isValid()) {
            throw new WrongArgumentException("Your annotation cannot be self-intersected.");
        }

        CommandResponse result = null;
        Geometry oldLocation = based.getLocation();
        if (remove) {
            log.info("doCorrectUserAnnotation : remove");
            //diff will be made
            //-remove the new geometry from the based annotation location
            //-remove the new geometry from all other annotation location
            based.setLocation(based.getLocation().difference(newGeometry));
            if (based.getLocation().getNumPoints() < 2) {
                throw new WrongArgumentException("You cannot delete an annotation with substract! Use reject or delete tool.");
            }

            JsonObject jsonObject = based.toJsonObject();
            based.setLocation(oldLocation);
            result = update(based, jsonObject);

            for(int i = 0; i < allAnnotationWithSameTerm.size(); i++) {
                ReviewedAnnotation other = allAnnotationWithSameTerm.get(i);
                other.setLocation(other.getLocation().difference(newGeometry));
                update(other, other.toJsonObject());
            }
        } else {
            log.info("doCorrectUserAnnotation : union");
            //union will be made:
            // -add the new geometry to the based annotation location.
            // -add all other annotation geometry to the based annotation location (and delete other annotation)
            based.setLocation(based.getLocation().union(newGeometry));
            for (ReviewedAnnotation other : allAnnotationWithSameTerm) {
                based.setLocation(based.getLocation().union(other.getLocation()));
                delete(other, null, null, false);
            }

            JsonObject jsonObject = based.toJsonObject();
            based.setLocation(oldLocation);
            result = update(based, jsonObject);
        }
        return result;
    }
}
