package be.cytomine.service.image;

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
import be.cytomine.domain.ontology.AnnotationTrack;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.repository.image.SliceInstanceRepository;
import be.cytomine.repository.ontology.AnnotationIndexRepository;
import be.cytomine.repository.ontology.AnnotationTrackRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.ontology.AnnotationTrackService;
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

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class SliceInstanceService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private SliceInstanceRepository sliceInstanceRepository;

    @Autowired
    private AnnotationTrackService annotationTrackService;

    @Autowired
    private AnnotationIndexRepository annotationIndexRepository;

    @Autowired
    private AnnotationTrackRepository annotationTrackRepository;

    @Override
    public Class currentDomain() {
        return SliceInstance.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new SliceInstance().buildDomainFromJson(json, getEntityManager());
    }

    public Optional<SliceInstance> find(Long id) {
        Optional<SliceInstance> sliceInstance = sliceInstanceRepository.findById(id);
        sliceInstance.ifPresent(cf -> {
            securityACLService.check(cf.container(),READ);
        });
        return sliceInstance;
    }

    public SliceInstance get(Long id) {
        return find(id).orElse(null);
    }



    public Optional<SliceInstance> find(ImageInstance image, int c, int z, int t) {
        Optional<SliceInstance> optionalSliceInstance = sliceInstanceRepository.findByCZT(image, c, z, t);
        optionalSliceInstance.ifPresent(cf -> {
            securityACLService.check(cf.container(), READ);
        });
        return optionalSliceInstance;
    }

    public List<SliceInstance> list(ImageInstance image) {
        securityACLService.check(image.container(), READ);
        return sliceInstanceRepository.listByImageInstanceOrderedByCZT(image);
    }


    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(json.getJSONAttrLong("project"), Project.class, READ);
        securityACLService.checkIsNotReadOnly(json.getJSONAttrLong("project"), Project.class);
        return executeCommand(new AddCommand(currentUser),null, json);

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
        securityACLService.check(domain.container(),READ);
        securityACLService.checkUser(currentUser);
        securityACLService.check(jsonNewData.getJSONAttrLong("project"), Project.class, READ);
        securityACLService.checkIsNotReadOnly(domain.container());
        securityACLService.checkIsNotReadOnly(jsonNewData.getJSONAttrLong("project"), Project.class);
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
        securityACLService.check(domain.container(),READ); // TODO?????
        securityACLService.checkUser(currentUser);
        securityACLService.checkFullOrRestrictedForOwner(domain.container(), ((SliceInstance)domain).getImage().getUser());
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {
        SliceInstance sliceInstance = (SliceInstance)domain;
        Optional<SliceInstance> alreadyExist = sliceInstanceRepository.findByBaseSliceAndImage(sliceInstance.getBaseSlice(), sliceInstance.getImage());
        if (alreadyExist.isPresent() && (!Objects.equals(alreadyExist.get().getId(), domain.getId()))) {
            throw new AlreadyExistException("SliceInstance (C: " + sliceInstance.getBaseSlice().getChannel() + ", Z:" + sliceInstance.getBaseSlice().getZStack() + ", T:" + sliceInstance.getBaseSlice().getTime() + " already exists for ImageInstance " + sliceInstance.getImage().getId());
        }
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        SliceInstance sliceInstance = (SliceInstance) domain;
        deleteDependentAnnotationTrack(sliceInstance, transaction, task);
        deleteDependentAnnotationIndex(sliceInstance, transaction, task);
    }

    private void deleteDependentAnnotationTrack(SliceInstance slice, Transaction transaction, Task task) {
        for (AnnotationTrack annotationTrack : annotationTrackRepository.findAllBySlice(slice)) {
            annotationTrackService.delete(annotationTrack, transaction, task, false);
        }
    }

    private void deleteDependentAnnotationIndex(SliceInstance slice, Transaction transaction, Task task) {
        annotationIndexRepository.deleteAllBySlice(slice);
    }


    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(),
                ((SliceInstance)domain).getBaseSlice().getChannel(),
                ((SliceInstance)domain).getBaseSlice().getZStack(),
                ((SliceInstance)domain).getBaseSlice().getTime()
                );
    }

}
