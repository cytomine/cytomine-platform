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
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.AnnotationTrack;
import be.cytomine.domain.ontology.Track;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.repository.ontology.AnnotationTrackRepository;
import be.cytomine.repository.ontology.TrackRepository;
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

import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class AnnotationTrackService extends ModelService {

    @Autowired
    private AnnotationTrackRepository annotationTrackRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AnnotationDomainRepository annotationDomainRepository;

    @Autowired
    private TrackRepository trackRepository;


    @Override
    public Class currentDomain() {
        return AnnotationTrack.class;
    }


    public AnnotationTrack get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<AnnotationTrack> find(Long id) {
        Optional<AnnotationTrack> optionalAnnotationTrack = annotationTrackRepository.findById(id);
        optionalAnnotationTrack.ifPresent(AnnotationTrack -> securityACLService.check(AnnotationTrack.container(),READ));
        return optionalAnnotationTrack;
    }

    public Optional<AnnotationTrack> find(AnnotationDomain annotation, Track track) {
        Optional<AnnotationTrack> optionalAnnotationTrack = annotationTrackRepository.findByAnnotationIdentAndTrack(annotation.getId(), track);
        optionalAnnotationTrack.ifPresent(AnnotationTrack -> securityACLService.check(AnnotationTrack.container(),READ));
        return optionalAnnotationTrack;
    }

    public List<AnnotationTrack> list(Track track) {
        securityACLService.check(track.container(),READ);
        return annotationTrackRepository.findAllByTrack(track);
    }

    public List<AnnotationTrack> list(AnnotationDomain annotation) {
        securityACLService.check(annotation.container(),READ);
        return annotationTrackRepository.findAllByAnnotationIdent(annotation.getId());
    }

    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    public CommandResponse add(JsonObject jsonObject) {
        AnnotationDomain annotation = annotationDomainRepository.findById(jsonObject.getJSONAttrLong("annotationIdent"))
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", jsonObject.getJSONAttrStr("annotationIdent")));
        securityACLService.check(annotation.container(),READ);
        securityACLService.checkFullOrRestrictedForOwner(annotation, annotation.user());
        securityACLService.checkUser(currentUserService.getCurrentUser());

        jsonObject.put("slice", annotation.getSlice().getId());
        jsonObject.put("annotationIdent", annotation.getId());
        jsonObject.put("annotationClassName", annotation.getClass().getName());

        return executeCommand(new AddCommand(currentUserService.getCurrentUser()), null, jsonObject);
    }

    public CommandResponse addAnnotationTrack(String annotationClassName, Long annotationIdent, Long idTrack, Long idSlice, Transaction transaction) {
        JsonObject jsonObject = JsonObject.of(
                "annotationClassName", annotationClassName,
                "annotationIdent", annotationIdent,
                "track", idTrack,
                "slice", idSlice);
        return executeCommand(new AddCommand(currentUserService.getCurrentUser(), transaction), null,jsonObject);
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
        AnnotationTrack annotationTrack = (AnnotationTrack)domain;
        AnnotationDomain annotation = annotationDomainRepository.findById(annotationTrack.getAnnotationIdent())
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", annotationTrack.getId()));

        securityACLService.check(annotation.container(), READ);
        securityACLService.checkFullOrRestrictedForOwner(annotation, annotation.user());
        securityACLService.checkUser(currentUserService.getCurrentUser());

        User currentUser = currentUserService.getCurrentUser();
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }


    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        AnnotationTrack annotationTrack = (AnnotationTrack)domain;
        return Arrays.asList(String.valueOf(annotationTrack.getId()), String.valueOf(annotationTrack.getAnnotationIdent()), annotationTrack.getTrack().getName());
    }


    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new AnnotationTrack().buildDomainFromJson(json, getEntityManager());
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain){
        AnnotationTrack annotationTrack = (AnnotationTrack)domain;
        if(annotationTrack!=null) {
            if(annotationTrackRepository.findByAnnotationIdentAndTrack(annotationTrack.getId(), annotationTrack.getTrack()).stream().anyMatch(x -> !Objects.equals(x.getId(), annotationTrack.getId())))  {
                throw new AlreadyExistException("AnnotationTrack already exists for annotation " + annotationTrack.getId() + " and track " + annotationTrack.getTrack().getName());
            }
            if(annotationTrackRepository.findBySliceAndTrack(annotationTrack.getSlice(), annotationTrack.getTrack()).stream().anyMatch(x -> !Objects.equals(x.getId(), annotationTrack.getId())))  {
                throw new AlreadyExistException("AnnotationTrack already exists for slice " + annotationTrack.getSlice().getId() + " and track " + annotationTrack.getTrack().getName());
            }
        }
    }


    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        throw new RuntimeException("Update is not implemented for shared annotation");
    }

    @Override
    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
    }

    @Override
    public CytomineDomain retrieve(JsonObject json) {

        CytomineDomain domain = null;
        if(json.containsKey("id") && !json.get("id").toString().equals("null")) {
            domain = (CytomineDomain) getEntityManager().find(currentDomain(), super.retrieveLongId(json)); // cast to long = issue ? TODO
        } else if (json.containsKey("annotationIdent") && json.containsKey("track")) {
            Track track = trackRepository.getById(json.getJSONAttrLong("track"));
            domain = annotationTrackRepository.findByAnnotationIdentAndTrack(json.getJSONAttrLong("annotationIdent"), track)
                    .orElse(null);
        }

        if (domain == null) {
            throw new ObjectNotFoundException(currentDomain() + " " + json + " not found");
        }
        CytomineDomain container = domain.container();
        if (container!=null) {
            //we only check security if container is defined
            securityACLService.check(container,READ);
        }
        return domain;
    }
}
