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
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ConstraintException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.*;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.meta.AttachedFileService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.*;
import be.cytomine.utils.filters.SQLSearchParameter;
import be.cytomine.utils.filters.SearchParameterEntry;
import be.cytomine.utils.filters.SpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Join;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
public class AbstractImageService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private CurrentRoleService currentRoleService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private AbstractImageRepository abstractImageRepository;

    @Autowired
    private ImageInstanceRepository imageInstanceRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ImageInstanceService imageInstanceService;

    @Autowired
    private CompanionFileRepository companionFileRepository;

    @Autowired
    private AbstractSliceRepository abstractSliceRepository;

    @Autowired
    private AbstractSliceService abstractSliceService;

    @Autowired
    private CompanionFileService companionFileService;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private NestedImageInstanceRepository nestedImageInstanceRepository;

    @Autowired
    private AttachedFileService attachedFileService;


    @Override
    public Class currentDomain() {
        return AbstractImage.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new AbstractImage().buildDomainFromJson(json, getEntityManager());
    }


    public Optional<AbstractImage> find(Long id) {
        Optional<AbstractImage> abstractImage = abstractImageRepository.findById(id);
        abstractImage.ifPresent(image -> securityACLService.check(image.container(),READ));
        return abstractImage;
    }

    public Optional<AbstractImage> find(Long id, String authHeader) {
        Optional<AbstractImage> abstractImage = abstractImageRepository.findById(id);
        String token = authHeader.replace("Bearer ", "");
        String username = TokenUtils.getUsernameFromToken(token);
        User user = currentUserService.getCurrentUser(username);
        abstractImage.ifPresent(image -> securityACLService.check(image.container(),READ, user));
        return abstractImage;
    }

    public Optional<AbstractImage> findByUploadedFile(Long id) {
        UploadedFile uploadedFile = uploadedFileRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("UploadedFile", id));
        Optional<AbstractImage> abstractImage = abstractImageRepository.findAllByUploadedFile(uploadedFile).stream().findAny();
        abstractImage.ifPresent(image -> securityACLService.check(image.container(),READ));
        return abstractImage;
    }

    public AbstractImage get(Long id) {
        return find(id).orElse(null);
    }


    public User getImageUploader(Long abstractImageId) {
        AbstractImage abstractImage = find(abstractImageId).orElseThrow(() -> new ObjectNotFoundException("AbstractImage", abstractImageId));
        return Optional.ofNullable(abstractImage.getUploadedFile()).map(UploadedFile::getUser).orElse(null);
    }

    /**
     * Check if some instances of this image exists and are still active
     */
    public boolean isAbstractImageUsed(Long abstractImageId) {
        AbstractImage domain = find(abstractImageId).orElseThrow(() -> new ObjectNotFoundException("AbstractImage", abstractImageId));
        return isAbstractImageUsed(domain);
    }

    private boolean isAbstractImageUsed(AbstractImage abstractImage) {
        boolean usedByImageInstance = imageInstanceRepository.existsByBaseImage(abstractImage);
        boolean usedByNestedFile = companionFileRepository.existsByImage(abstractImage);

        return usedByImageInstance || usedByNestedFile;
    }

    /**
     * Returns the list of all the unused abstract images
     */
    // TODO: bad perf!
    public List<AbstractImage> listUnused() {
        return list().getContent().stream().filter(x -> !isAbstractImageUsed(x)).collect(Collectors.toList());
    }

    public Page<AbstractImage> list() {
        return list(null, new ArrayList<>(), Pageable.unpaged());
    }

    public Page<AbstractImage> list(Project project, List<SearchParameterEntry> searchParameters, Pageable pageable) {
        List<SearchParameterEntry> validSearchParameters = SQLSearchParameter.getDomainAssociatedSearchParameters(AbstractImage.class, searchParameters, getEntityManager());

        Specification<AbstractImage> specification = SpecificationBuilder.getSpecificationFromFilters(validSearchParameters);

        if (!currentRoleService.isAdminByNow(currentUserService.getCurrentUser())) {
            List<Storage> storages = securityACLService.getStorageList(currentUserService.getCurrentUser(), false);
            Specification<AbstractImage> filterStorages = (root, query, criteriaBuilder) -> {
                    Join<AbstractImage, UploadedFile> uploadedFileJoin = root.join(AbstractImage_.uploadedFile);
                    return criteriaBuilder.in(uploadedFileJoin.get("storage")).value(storages);
            };
            specification = specification.and(filterStorages);
        }
        Page<AbstractImage> images = abstractImageRepository.findAll(specification, pageable);

        if (project != null) {
            TreeSet<Long> inProjectImagesId = new TreeSet<>(imageInstanceRepository.findAllByProject(project).stream().map(x -> x.getBaseImage().getId())
                    .collect(Collectors.toList()));
            for (AbstractImage abstractImage : images.getContent()) {
                abstractImage.setInProject(inProjectImagesId.contains(abstractImage.getId()));
            }
        }

        return images;
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        transactionService.start();
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

        if (!json.isMissing("uploadedFile")) {
            //TODO: ???
        }
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

        JsonObject versionBeforeUpdate = domain.toJsonObject();

        CommandResponse commandResponse = executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
        AbstractImage abstractImage = (AbstractImage)commandResponse.getObject();

        Integer magnification = versionBeforeUpdate.getJSONAttrInteger("magnification",null);
        Double physicalSizeX = versionBeforeUpdate.getJSONAttrDouble("physicalSizeX",null);

        boolean magnificationUpdated = !Objects.equals(magnification, abstractImage.getMagnification());
        boolean physicalSizeXUpdated = !Objects.equals(physicalSizeX, abstractImage.getPhysicalSizeX());

        List<ImageInstance> images = new ArrayList<>();
        if(physicalSizeXUpdated && magnificationUpdated ) {
            if(physicalSizeX!= null && magnification!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(abstractImage, x -> physicalSizeX.equals(x.getPhysicalSizeX()) && magnification.equals(x.getMagnification())));
            } else if(physicalSizeX!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(abstractImage, x -> physicalSizeX.equals(x.getPhysicalSizeX()) && x.getMagnification()==null));
            } else if(magnification!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(abstractImage, x -> magnification.equals(x.getMagnification()) && x.getPhysicalSizeX()==null));
            } else {
                images.addAll(imageInstanceRepository.findAllByBaseImage(abstractImage, x -> x.getMagnification()==null && x.getPhysicalSizeX()==null));
            }
            for (ImageInstance image : images) {
                JsonObject json = image.toJsonObject();
                json.put("physicalSizeX", abstractImage.getPhysicalSizeX());
                json.put("magnification", abstractImage.getMagnification());
                imageInstanceService.update(image, json);
            }
        } else if (physicalSizeXUpdated) {
            if(physicalSizeX!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(abstractImage, x -> physicalSizeX.equals(x.getPhysicalSizeX()) ));
            } else {
                images.addAll(imageInstanceRepository.findAllByBaseImage(abstractImage, x -> x.getPhysicalSizeX()==null));
            }

            for (ImageInstance image : images) {
                JsonObject json = image.toJsonObject();
                json.put("physicalSizeX", abstractImage.getPhysicalSizeX());
                imageInstanceService.update(image, json);
            }
        }
        if(magnificationUpdated) {
            if(magnification!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(abstractImage, x -> magnification.equals(x.getMagnification()) ));
            } else {
                images.addAll(imageInstanceRepository.findAllByBaseImage(abstractImage, x -> x.getMagnification()==null));
            }

            for (ImageInstance image : images) {
                JsonObject json = image.toJsonObject();
                json.put("magnification", abstractImage.getMagnification());
                imageInstanceService.update(image, json);
            }
        }
        return commandResponse;
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

        if (!isAbstractImageUsed(domain.getId())) {
            Command c = new DeleteCommand(currentUser, transaction);
            return executeCommand(c,domain, null);
        } else {
            List<ImageInstance> instances = imageInstanceRepository.findAllByBaseImage((AbstractImage) domain);
            throw new ForbiddenException("Abstract Image has instances in active projects : " +
                    instances.stream().map(x -> x.getProject().getName()).collect(Collectors.joining(",")) +
                    " with the following names : " +
                    instances.stream().map(x -> x.getInstanceFilename()).distinct().collect(Collectors.joining(",")),
                    Map.of("projectNames", instances.stream().map(x -> x.getProject().getName()).collect(Collectors.toList()), "imageNames", instances.stream().map(x -> x.getInstanceFilename()).distinct().collect(Collectors.toList())));
        }


    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), StringUtils.getBlankIfNull(((AbstractImage) domain).getOriginalFilename()));
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentAbstractSlice((AbstractImage)domain, transaction, task);
        deleteDependentImageInstance((AbstractImage)domain, transaction, task);
        deleteDependentCompanionFile((AbstractImage)domain, transaction, task);
        deleteDependentAttachedFile((AbstractImage)domain, transaction, task);
        deleteDependentNestedImageInstance((AbstractImage)domain, transaction, task);
    }

    private void  deleteDependentAbstractSlice(AbstractImage ai, Transaction transaction, Task task) {
        List<AbstractSlice> slices = abstractSliceRepository.findAllByImage(ai);
        for (AbstractSlice slice : slices) {
            abstractSliceService.delete(slice, transaction, task, false);
        }

    }

    private void deleteDependentImageInstance(AbstractImage ai, Transaction transaction,Task task) {
        List<ImageInstance> images = imageInstanceRepository.findAllByBaseImage(ai);
        if(!images.isEmpty()) {
            throw new ConstraintException("This image cannot be deleted as it has already been insert " +
                    "in projects " + images.stream().map(x -> x.getProject().getName()).collect(Collectors.joining(",")));
        }
    }

    private void deleteDependentCompanionFile (AbstractImage ai, Transaction transaction, Task task) {
        List<CompanionFile> companionFiles = companionFileRepository.findAllByImage(ai);
        for (CompanionFile companionFile : companionFiles) {
            companionFileService.delete(companionFile, transaction, task, false);
        }
    }

    private void deleteDependentAttachedFile(AbstractImage ai, Transaction transaction,Task task)  {
        List<AttachedFile> attachedFiles = attachedFileService.findAllByDomain(ai);
        for (AttachedFile attachedFile : attachedFiles) {
            attachedFileService.delete(attachedFile, transaction, task, false);
        }
    }

    private void  deleteDependentNestedImageInstance(AbstractImage ai, Transaction transaction,Task task) {
        List<NestedImageInstance> nestedImageInstances = nestedImageInstanceRepository.findAllByBaseImage(ai);
        for(NestedImageInstance nestedImageInstance : nestedImageInstances) {
            nestedImageInstanceRepository.delete(nestedImageInstance);
        }
    }
}
