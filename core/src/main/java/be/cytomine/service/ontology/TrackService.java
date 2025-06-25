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
import be.cytomine.domain.ontology.AnnotationTrack;
import be.cytomine.domain.ontology.Track;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.TrackRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
public class TrackService extends ModelService {

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ImageInstanceService imageInstanceService;

    @Autowired
    private AnnotationTrackService annotationTrackService;

    @Override
    public Class currentDomain() {
        return Track.class;
    }

    public Track get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<Track> find(Long id) {
        Optional<Track> optionalTrack = trackRepository.findById(id);
        optionalTrack.ifPresent(track -> securityACLService.check(track.container(),READ));
        return optionalTrack;
    }

    public List<Track> list(ImageInstance imageInstance) {
        securityACLService.check(imageInstance.container(),READ);
        return trackRepository.findAllByImage(imageInstance);
    }

    public List<Track> list(Project project) {
        securityACLService.check(project,READ);
        return trackRepository.findAllByProject(project);
    }

    public Long countByProject(Project project, Date startDate, Date endDate) {
        if (startDate!=null && endDate!=null) {
            return trackRepository.countByProjectAndCreatedBetween(project, startDate, endDate);
        } else if (startDate!=null) {
            return trackRepository.countByProjectAndCreatedAfter(project, startDate);
        } else if (endDate!=null) {
            return trackRepository.countByProjectAndCreatedBefore(project, endDate);
        } else {
            return trackRepository.countByProject(project);
        }
    }

    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    public CommandResponse add(JsonObject jsonObject) {
        ImageInstance imageInstance = imageInstanceService.find(jsonObject.getJSONAttrLong("image",0L))
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", jsonObject.getJSONAttrStr("image")));
        jsonObject.put("project", imageInstance.getProject().getId());

        securityACLService.check(imageInstance.getProject(), READ);
        securityACLService.checkFullOrRestrictedForOwner(imageInstance, imageInstance.getUser());
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

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
        Track track = ((Track)domain);
        securityACLService.check(domain.container(),READ);
        securityACLService.checkFullOrRestrictedForOwner(track, track.getImage().getUser());
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

        ImageInstance imageInstance = imageInstanceService.find(jsonNewData.getJSONAttrLong("image", 0L))
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", jsonNewData.getJSONAttrStr("image")));
        jsonNewData.put("project", imageInstance.getProject().getId());

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
        Track track = ((Track)domain);
        securityACLService.check(domain.container(),READ);
        securityACLService.checkFullOrRestrictedForOwner(track, track.getImage().getUser());
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }


    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Track().buildDomainFromJson(json, getEntityManager());
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain){
        Track track = (Track)domain;
        if(track!=null && track.getName()!=null) {
            if(trackRepository.findByNameAndImage(track.getName(), track.getImage()).stream().anyMatch(x -> !Objects.equals(x.getId(), track.getId())))  {
                throw new AlreadyExistException("Track " + track.getName() + " already exist in this image!");
            }
        }
    }

    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        Track track = (Track)domain;
        return Arrays.asList(String.valueOf(track.getId()), track.getName());
    }

    @Override
    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentAnnotationTrack((Track)domain, transaction, task);
    }

    public void deleteDependentAnnotationTrack(Track track, Transaction transaction, Task task) {
        for (AnnotationTrack annotationTrack : annotationTrackService.list(track)) {
            annotationTrackService.delete(annotationTrack, transaction, null, false);
        }
    }

}
