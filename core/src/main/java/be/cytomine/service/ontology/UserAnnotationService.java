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
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.annotation.AnnotationLight;
import be.cytomine.dto.annotation.SimplifiedAnnotation;
import be.cytomine.dto.image.BoundariesCropParameter;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.UserAnnotationListing;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.repository.image.SliceInstanceRepository;
import be.cytomine.repository.ontology.SharedAnnotationRepository;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.image.SliceCoordinatesService;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.meta.PropertyService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.SimplifyGeometryService;
import be.cytomine.service.utils.ValidateGeometryService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.GeometryUtils;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import be.cytomine.utils.TokenUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class UserAnnotationService extends ModelService {

    @Autowired
    private AnnotationLinkService annotationLinkService;

    @Autowired
    private UserAnnotationRepository userAnnotationRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AnnotationListingService annotationListingService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SliceInstanceService sliceInstanceService;

    @Autowired
    private SimplifyGeometryService simplifyGeometryService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ValidateGeometryService validateGeometryService;

    @Autowired
    private AnnotationTermService annotationTermService;

    @Autowired
    private GenericAnnotationService genericAnnotationService;

    @Autowired
    private SliceCoordinatesService sliceCoordinatesService;

    @Autowired
    private ImageInstanceRepository imageInstanceRepository;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private AnnotationTrackService annotationTrackService;

    @Autowired
    private SliceInstanceRepository sliceInstanceRepository;

    @Autowired
    private SharedAnnotationService sharedAnnotationService;

    @Autowired
    private SharedAnnotationRepository sharedAnnotationRepository;

    @Override
    public Class currentDomain() {
        return UserAnnotation.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new UserAnnotation().buildDomainFromJson(json, entityManager);
    }

    public UserAnnotation get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<UserAnnotation> find(Long id) {
        Optional<UserAnnotation> optionalUserAnnotation = userAnnotationRepository.findById(id);
        optionalUserAnnotation.ifPresent(userAnnotation -> securityACLService.check(userAnnotation.container(),READ));
        return optionalUserAnnotation;
    }

    public Optional<UserAnnotation> find(Long id, String authHeader) {
        Optional<UserAnnotation> userAnnotation = userAnnotationRepository.findById(id);
        String token = authHeader.replace("Bearer ", "");
        String username = TokenUtils.getUsernameFromToken(token);
        User user = currentUserService.getCurrentUser(username);
        userAnnotation.ifPresent(annotation -> securityACLService.check(annotation.container(),READ, user));
        return userAnnotation;
    }

    public List listIncluded(ImageInstance image, String geometry, User user, List<Long> terms, AnnotationDomain annotation, List<String> propertiesToShow) {
        securityACLService.check(image.container(), READ);

        UserAnnotationListing userAnnotationListing = new UserAnnotationListing(entityManager);
        userAnnotationListing.setColumnsToPrint(propertiesToShow);
        userAnnotationListing.setImage(image.getId());
        userAnnotationListing.setUser(user.getId());
        userAnnotationListing.setTerms(terms);
        userAnnotationListing.setExcludedAnnotation((annotation!=null? annotation.getId() : null));
        userAnnotationListing.setBbox(geometry);
        return annotationListingService.executeRequest(userAnnotationListing);
    }


    public Long count(User user, Project project) {
        if (project!=null) {
            securityACLService.checkIsSameUserOrAdminContainer(project, user, currentUserService.getCurrentUser());
            return userAnnotationRepository.countByUserAndProject(user, project);
        } else {
            securityACLService.checkIsSameUser(user, currentUserService.getCurrentUser());
            return userAnnotationRepository.countByUser(user);
        }
    }


    public Long countByProject(Project project) {
        return countByProject(project, null, null);
    }

    public Long countByProject(Project project, Date startDate, Date endDate) {
        if (startDate!=null && endDate!=null) {
            return userAnnotationRepository.countByProjectAndCreatedBetween(project, startDate, endDate);
        } else if (startDate!=null) {
            return userAnnotationRepository.countByProjectAndCreatedAfter(project, startDate);
        } else if (endDate!=null) {
            return userAnnotationRepository.countByProjectAndCreatedBefore(project, endDate);
        } else {
            return userAnnotationRepository.countByProject(project);
        }
    }

    /**
     * List all annotation with a very light structure: id, project and crop url
     */
    public List<AnnotationLight> listLight() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return userAnnotationRepository.listLight();
    }


    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    public CommandResponse add(JsonObject jsonObject) {
        SliceInstance slice = null;
        ImageInstance image = null;
        // TODO perf: we load twice slice/image/... : here + in userannotation domain "build from json" method
        // Same for user
        // We could provide "sliceObject", "imageObject", "userObject" in JSON and first check for them in the userannotation class
        if (!jsonObject.isMissing("slice")) {
            slice = sliceInstanceService.find(jsonObject.getJSONAttrLong("slice"))
                    .orElseThrow(() -> new ObjectNotFoundException("SliceInstance with id " + jsonObject.get("slice")));
            image = slice.getImage();
        } else if (!jsonObject.isMissing("image")) {
            image = imageInstanceRepository.findById(jsonObject.getJSONAttrLong("image"))
                    .orElseThrow(() -> new ObjectNotFoundException("ImageInstance with id " + jsonObject.get("image")));
            slice = sliceCoordinatesService.getReferenceSlice(image);

        } else {
            throw new WrongArgumentException("Cannot retrieve slice or image");
        }
        Project project = slice.getProject();

        if (jsonObject.isMissing("location")) {
            throw new WrongArgumentException("Annotation must have a valid geometry:" + jsonObject.get("location"));
        }

        jsonObject.put("sliceObject", slice);
        jsonObject.put("imageObject", image);
        jsonObject.put("projectObject", project);

        User currentUser = currentUserService.getCurrentUser();

        //Check if user has at least READ permission for the project
        securityACLService.check(project, READ, currentUser);
        //Check if project EditingMode is not READ_ONLY
        securityACLService.checkIsNotReadOnly(project);
        //Check if user has a role that allows to create annotations
        securityACLService.checkGuest(currentUser);
        //If user info is missing from input, add it
        if (jsonObject.isMissing("user")) {
            jsonObject.put("user", currentUser.getId());
            jsonObject.put("userObject", currentUser);
            // check if user is the owner of the annotation, if not check project editing mode and user role
        } else if (!Objects.equals(jsonObject.getJSONAttrLong("user"), currentUser.getId())) {
            securityACLService.checkFullOrRestrictedForOwner(project, null);
        }

        Geometry annotationShape;
        try {
            annotationShape = new WKTReader().read(jsonObject.getJSONAttrStr("location"));
        }
        catch (Exception ignored) {
            throw new WrongArgumentException("Annotation location is not valid");
        }

        if (!annotationShape.isValid()) {
            throw new WrongArgumentException("Annotation location is not valid");
        }


        Envelope envelope = annotationShape.getEnvelopeInternal();
        boolean isSizeDefined = image.getBaseImage().getWidth()!=null && image.getBaseImage().getHeight()!=null;
        if (isSizeDefined && (envelope.getMinX() < 0 || envelope.getMinY() < 0 ||
                envelope.getMaxX() > image.getBaseImage().getWidth() ||
                envelope.getMaxY() > image.getBaseImage().getHeight())) {
            double maxX = Math.min(envelope.getMaxX(), image.getBaseImage().getWidth());
            double maxY = Math.min(envelope.getMaxY(), image.getBaseImage().getHeight());
            Geometry insideBounds = null;
            try {
                insideBounds = new WKTReader().read("POLYGON((0 0,0 " + maxY + "," + maxX + " " + maxY + "," + maxX + " 0,0 0))");
            } catch (ParseException e) {
                throw new WrongArgumentException("Annotation cannot be parsed with maxX/maxY:" + e.getMessage());
            }
            annotationShape = annotationShape.intersection(insideBounds);
        }

        if(!(annotationShape.getGeometryType().equals("LineString"))) {
            BoundariesCropParameter boundaries = GeometryUtils.getGeometryBoundaries(annotationShape);
            if (boundaries == null || boundaries.getWidth() == 0 || boundaries.getHeight() == 0) {
                throw new WrongArgumentException("Annotation dimension not valid");
            }
        }

        //simplify annotation
        try {
            SimplifiedAnnotation simplifiedAnnotation =
                    simplifyGeometryService.simplifyPolygon(annotationShape, jsonObject.getJSONAttrLong("minPoint", null), jsonObject.getJSONAttrLong("maxPoint", null));
            jsonObject.put("location", simplifiedAnnotation.getNewAnnotation());
            jsonObject.put("geometryCompression", simplifiedAnnotation.getRate());
        } catch (Exception e) {
            log.error("Cannot simplify annotation location:" + e);
        }

        if (jsonObject.isMissing("location")) {
            jsonObject.put("location", annotationShape);
            jsonObject.put("geometryCompression", 0.0d);
        }

        if (jsonObject.get("location") instanceof Geometry) {
            jsonObject.put("location", validateGeometryService.tryToMakeItValidIfNotValid((Geometry)jsonObject.get("location")));
        } else {
            jsonObject.put("location", validateGeometryService.tryToMakeItValidIfNotValid(jsonObject.getJSONAttrStr("location")));
        }

        //Start transaction
        Transaction transaction = transactionService.start();

        CommandResponse commandResponse = executeCommand(new AddCommand(currentUser, transaction), null, jsonObject);
        UserAnnotation addedAnnotation = (UserAnnotation)commandResponse.getObject();

        if (addedAnnotation == null) {
            return commandResponse;
        }


        // Add annotation-term if any
        List<Long> termIds = new ArrayList<>();
        termIds.addAll(jsonObject.getJSONAttrListLong("term", new ArrayList<>()));
        termIds.addAll(jsonObject.getJSONAttrListLong("terms", new ArrayList<>()));
        log.debug("add terms if presents");
        List<Term> terms = new ArrayList<>();
        for (Long termId : termIds) {

            CommandResponse response = annotationTermService.addAnnotationTerm(addedAnnotation.getId(), termId, null, currentUser.getId(), currentUser, transaction);
            terms.add(((AnnotationTerm)(response.getObject())).getTerm());
        }

        ((Map<String, Object>)commandResponse.getData().get("annotation")).put("term", terms.stream().map(x -> x.toJsonObject().getId()).toList());


        // Add properties if any
        Map<String, String> properties = new HashMap<>();
        properties.putAll(jsonObject.getJSONAttrMapString("property", new HashMap<>()));
        properties.putAll(jsonObject.getJSONAttrMapString("properties", new HashMap<>()));

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            propertyService.addProperty(UserAnnotation.class.getName(), addedAnnotation.getId(), key, value, currentUser, transaction);
        }

        List<Long> tracksIds = new ArrayList<>();
        tracksIds.addAll(jsonObject.getJSONAttrListLong("track", new ArrayList<>()));
        tracksIds.addAll(jsonObject.getJSONAttrListLong("tracks", new ArrayList<>()));
        if (!tracksIds.isEmpty()) {

            List<AnnotationTrack> annotationTracks = new ArrayList<>();
            for (Long trackId : tracksIds) {
                CommandResponse response =
                        annotationTrackService.addAnnotationTrack(UserAnnotation.class.getName(), addedAnnotation.getId(), trackId, addedAnnotation.getSlice().getId(), transaction);
                annotationTracks.add((AnnotationTrack) response.getData().get("annotationtrack"));
            }
            ((Map<String, Object>)commandResponse.getData().get("annotation")).put("annotationTrack", annotationTracks);
            ((Map<String, Object>)commandResponse.getData().get("annotation")).put("track", annotationTracks.stream().map(x -> x.getTrack()).collect(Collectors.toList()));
        }

        // Add annotation-group/link if any
        Long groupId = jsonObject.getJSONAttrLong("group", null);
        if (groupId != null) {
            CommandResponse response = annotationLinkService.addAnnotationLink(
                    UserAnnotation.class.getName(),
                    addedAnnotation.getId(),
                    groupId,
                    addedAnnotation.getImage().getId(),
                    transaction
            );

            ((Map<String, Object>)commandResponse.getData().get("annotation")).put("group", groupId);
            ((Map<String, Object>)commandResponse.getData().get("annotation")).put("annotationLinks", response.getData().get("annotationlink"));
        }

        log.debug("end of add command");
        return commandResponse;
    }

    protected void beforeAdd(CytomineDomain domain) {
        // this will be done in the PrePersist method ; but the validation is done before PrePersist
        ((UserAnnotation)domain).setWktLocation(((UserAnnotation)domain).getLocation().toText());
    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        response.getData().put("annotation", response.getData().get("userannotation"));
        response.getData().remove("userannotation");
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return Response structure (new domain data, old domain data..)
     */
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        User currentUser = currentUserService.getCurrentUser();
        //Check if user has a role that allows to update annotations
        securityACLService.checkGuest(currentUser);
        //Check if user has at least READ permission for the project
        securityACLService.check(domain.container(), READ, currentUser);
        //Check if user is admin, the project mode and if is the owner of the annotation
        securityACLService.checkFullOrRestrictedForOwner(domain, ((UserAnnotation) domain).getUser());

        // TODO: what about image/project ??

        Geometry annotationShape;
        try {
            annotationShape = new WKTReader().read(jsonNewData.getJSONAttrStr("location"));
        }
        catch (ParseException ignored) {
            throw new WrongArgumentException("Annotation location is not valid");
        }

        if (!annotationShape.isValid()) {
            throw new WrongArgumentException("Annotation location is not valid");
        }

        ImageInstance image = imageInstanceRepository.findById(jsonNewData.getJSONAttrLong("image"))
                .orElseThrow(() -> new WrongArgumentException("Annotation not associated with a valid image"));

        Envelope envelope = annotationShape.getEnvelopeInternal();
        boolean isSizeDefined = image.getBaseImage().getWidth()!=null && image.getBaseImage().getHeight()!=null;
        if (isSizeDefined && (envelope.getMinX() < 0 || envelope.getMinY() < 0 ||
                envelope.getMaxX() > image.getBaseImage().getWidth() ||
                envelope.getMaxY() > image.getBaseImage().getHeight())) {
            double maxX = Math.min(envelope.getMaxX(), image.getBaseImage().getWidth());
            double maxY = Math.min(envelope.getMaxY(), image.getBaseImage().getHeight());
            Geometry insideBounds = null;
            try {
                insideBounds = new WKTReader().read("POLYGON((0 0,0 " + maxY + "," + maxX + " " + maxY + "," + maxX + " 0,0 0))");
            } catch (ParseException e) {
                throw new WrongArgumentException("Annotation cannot be parsed with maxX/maxY:" + e.getMessage());
            }
            annotationShape = annotationShape.intersection(insideBounds);
        }

        //simplify annotation
        try {
            double rate = jsonNewData.getJSONAttrDouble("geometryCompression", 0d);
            SimplifiedAnnotation data = simplifyGeometryService.simplifyPolygon(annotationShape, rate);
            jsonNewData.put("location", data.getNewAnnotation());
            jsonNewData.put("geometryCompression", data.getRate());
        } catch (Exception e) {
            log.error("Cannot simplify annotation location:" + e);
        }

        if (jsonNewData.get("location") instanceof Geometry) {
            jsonNewData.put("location", validateGeometryService.tryToMakeItValidIfNotValid((Geometry)jsonNewData.get("location")));
        } else {
            jsonNewData.put("location", validateGeometryService.tryToMakeItValidIfNotValid(jsonNewData.getJSONAttrStr("location")));
        }
        CommandResponse result = executeCommand(new EditCommand(currentUser, null), domain, jsonNewData);

        return result;
    }


    protected void afterUpdate(CytomineDomain domain, CommandResponse response) {
        String query = "UPDATE annotation_link SET updated = NOW() WHERE annotation_ident = " + domain.getId();
        getEntityManager().createNativeQuery(query);

        response.getData().put("annotation", response.getData().get("userannotation"));
        response.getData().remove("userannotation");
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
        //Check if user has a role that allows to delete annotations
        securityACLService.checkGuest(currentUser);
        //Check if user has at least READ permission for the project
        securityACLService.check(domain.container(), READ, currentUser);
        //Check if user is admin, the project mode and if is the owner of the annotation
        securityACLService.checkFullOrRestrictedForOwner(domain, ((UserAnnotation)domain).getUser());
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    protected void afterDelete(CytomineDomain domain, CommandResponse response) {
        response.getData().put("annotation", response.getData().get("userannotation"));
        response.getData().remove("userannotation");
    }

    public List<CommandResponse> repeat(UserAnnotation userAnnotation, Long baseSliceId, int repeat) {
        SliceInstance currentSlice = sliceInstanceService.find(baseSliceId)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance with id " + baseSliceId));

        List<SliceInstance> slices = sliceInstanceRepository.listByImageInstanceOrderedByTZC(
                userAnnotation.getImage(),
                currentSlice.getBaseSlice().getTime(),
                currentSlice.getBaseSlice().getZStack(),
                currentSlice.getBaseSlice().getChannel(),
                userAnnotation.getSlice().getId()
        ).stream().limit(repeat).collect(Collectors.toList());

        List<CommandResponse> collection = slices.stream()
                .map(slice -> this.add(JsonObject.of(
                        "slice", slice.getId(),
                        "location", userAnnotation.getLocation().toString(),
                        "terms", userAnnotation.termsId(),
                        "tracks", userAnnotation.tracksId())))
                .collect(Collectors.toList());

        return collection;
    }


    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        UserAnnotation annotation = (UserAnnotation)domain;
        return List.of(currentUserService.getCurrentUser().toString(), annotation.getImage().getBlindInstanceFilename(), ((UserAnnotation) domain).getUser().toString());
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentAnnotationTerm((UserAnnotation)domain, transaction, task);
        deleteDependentSharedAnnotation((UserAnnotation)domain, transaction, task);
        deleteDependentAnnotationTrack((UserAnnotation)domain, transaction, task);
        deleteDependentMetadata(domain, transaction, task);
    }


    public void deleteDependentAnnotationTerm(UserAnnotation ua, Transaction transaction, Task task) {
        for (AnnotationTerm annotationTerm : annotationTermService.list(ua)) {
            try {
                annotationTermService.delete(annotationTerm, transaction, null, false);
            } catch (ForbiddenException fe) {
                throw new ForbiddenException("This annotation has been linked to the term " + annotationTerm.getTerm() + " by " + annotationTerm.userDomainCreator() + ". " + annotationTerm.userDomainCreator() + " must unlink its term before you can delete this annotation.");
            }
        }
    }

    public void deleteDependentSharedAnnotation(UserAnnotation aa, Transaction transaction, Task task) {
        for (SharedAnnotation sharedAnnotation : sharedAnnotationRepository.findAllByAnnotationIdentOrderByCreatedDesc(aa.getId())) {
            sharedAnnotationService.delete(sharedAnnotation,transaction,task,false);
        }
    }
    public void deleteDependentAnnotationTrack(UserAnnotation ua, Transaction transaction, Task task) {
        for (AnnotationTrack annotationTrack : annotationTrackService.list(ua)) {
            annotationTrackService.delete(annotationTrack, transaction, task, false);
        }
    }

    public CommandResponse doCorrectUserAnnotation(List<Long> coveringAnnotations, String newLocation, boolean remove) throws ParseException {
        if (coveringAnnotations.isEmpty()) {
            return null;
        }

        //Get the based annotation
        UserAnnotation based = this.find(coveringAnnotations.get(0)).get();

        //Get the term of the based annotation, it will be the main term
        List<Long> basedTerms = based.termsId();

        //Get all other annotation with same term
        List<Long> allOtherAnnotationId = coveringAnnotations.subList(1, coveringAnnotations.size());
        List<UserAnnotation> allAnnotationWithSameTerm = genericAnnotationService.findUserAnnotationWithTerm(allOtherAnnotationId, basedTerms);

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
                UserAnnotation other = allAnnotationWithSameTerm.get(i);
                other.setLocation(other.getLocation().difference(newGeometry));
                update(other, other.toJsonObject());
            }
        } else {
            log.info("doCorrectUserAnnotation : union");
            //union will be made:
            // -add the new geometry to the based annotation location.
            // -add all other annotation geometry to the based annotation location (and delete other annotation)
            based.setLocation(based.getLocation().union(newGeometry));
            for (UserAnnotation other : allAnnotationWithSameTerm) {
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
