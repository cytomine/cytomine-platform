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
import be.cytomine.domain.image.*;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ConstraintException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.AbstractSliceRepository;
import be.cytomine.repository.image.SliceInstanceRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
public class AbstractSliceService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private AbstractSliceRepository abstractSliceRepository;

    @Autowired
    private SliceInstanceRepository sliceInstanceRepository;

    @Override
    public Class currentDomain() {
        return AbstractSlice.class;
    }

    public User findImageUploaded(Long abstractSliceId) {
        AbstractSlice abstractSlice = find(abstractSliceId).orElseThrow(() -> new ObjectNotFoundException("AbstractSlice", abstractSliceId));
        return Optional.ofNullable(abstractSlice.getUploadedFile()).map(UploadedFile::getUser).orElse(null);
    }


    public Optional<AbstractSlice> find(Long id) {
        Optional<AbstractSlice> abstractSlice = abstractSliceRepository.findById(id);
        abstractSlice.ifPresent(image -> securityACLService.check(image.container(),READ));
        return abstractSlice;
    }

    public AbstractSlice get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<AbstractSlice> find(AbstractImage abstractImage, Integer channel, Integer zStack, Integer time) {
        Optional<AbstractSlice> abstractSlice = abstractSliceRepository.findByImageAndChannelAndZStackAndTime(abstractImage, channel, zStack, time);
        abstractSlice.ifPresent(image -> securityACLService.check(image.container(),READ));
        return abstractSlice;
    }


    public List<AbstractSlice> list(AbstractImage image) {
        securityACLService.check(image, READ);
        return abstractSliceRepository.findAllByImage(image);
    }

    public List<AbstractSlice> list(UploadedFile uploadedFile) {
        securityACLService.check(uploadedFile, READ);
        return abstractSliceRepository.findAllByUploadedFile(uploadedFile);
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

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
        securityACLService.check(domain.container(),WRITE);

        if (!isAbstractSliceUsed(domain.getId())) {
            Command c = new DeleteCommand(currentUser, transaction);
            return executeCommand(c,domain, null);
        } else {
            List<SliceInstance> instances = sliceInstanceRepository.findAllByBaseSlice((AbstractSlice) domain);
            throw new ForbiddenException("Abstract Slice has instances in active projects: " +
                    instances.stream().map(x -> x.getProject().getName()).collect(Collectors.joining(",")) +
                    " with the following names : " +
                    instances.stream().map(x -> x.getBaseSlice().getImage().getOriginalFilename()).distinct().collect(Collectors.joining(",")),
                    Map.of("projectNames", instances.stream().map(x -> x.getProject().getName()).collect(Collectors.toList()), "imageNames", instances.stream().map(x -> x.getBaseSlice().getImage().getOriginalFilename()).distinct().collect(Collectors.toList())));
        }
    }

    public boolean isAbstractSliceUsed(Long abstractImageId) {
        AbstractSlice domain = find(abstractImageId).orElseThrow(() -> new ObjectNotFoundException("AbstractImage", abstractImageId));
        return isAbstractSliceUsed(domain);
    }
    private boolean isAbstractSliceUsed(AbstractSlice abstractSlice) {
        return sliceInstanceRepository.existsByBaseSlice(abstractSlice);
    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((AbstractSlice)domain).getChannel(), ((AbstractSlice)domain).getZStack(), ((AbstractSlice)domain).getTime());
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentSliceInstance((AbstractSlice)domain, transaction, task);
    }


    private void deleteDependentSliceInstance(AbstractSlice ai, Transaction transaction,Task task) {
        List<SliceInstance> images = sliceInstanceRepository.findAllByBaseSlice(ai);
        if(!images.isEmpty()) {
            throw new ConstraintException("This slice " + ai.getId()+ " cannot be deleted as it has already been insert " +
                    "in projects " + images.stream().map(x -> x.getProject().getName()).collect(Collectors.joining(",")));
        }
    }


    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new AbstractSlice().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {
        AbstractSlice abstractSlice = ((AbstractSlice)domain);
        abstractSliceRepository.findByImageAndChannelAndZStackAndTime(abstractSlice.getImage(), abstractSlice.getChannel(), abstractSlice.getZStack(), abstractSlice.getTime()).ifPresent(slice -> {
            if (!Objects.equals(slice.getId(), abstractSlice.getId())) {
                throw new AlreadyExistException("AbstractSlice (C:" + abstractSlice.getChannel() + ", Z:" + abstractSlice.getZStack() + ", T:" + abstractSlice.getTime() + ") already exists for AbstractImage " + abstractSlice.getImage().getId());
            }
        });

    }

}
