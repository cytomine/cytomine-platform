package be.cytomine.service.image.group;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.repository.image.group.ImageGroupImageInstanceRepository;
import be.cytomine.repository.image.group.ImageGroupRepository;
import be.cytomine.repository.ontology.AnnotationGroupRepository;
import be.cytomine.repository.ontology.AnnotationLinkRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class ImageGroupService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ImageGroupImageInstanceService imageGroupImageInstanceService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AnnotationGroupRepository annotationGroupRepository;

    @Autowired
    private AnnotationLinkRepository annotationLinkRepository;

    @Autowired
    private ImageGroupRepository imageGroupRepository;

    @Autowired
    private ImageGroupImageInstanceRepository imageGroupImageInstanceRepository;

    @Override
    public Class currentDomain() {
        return ImageGroup.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new ImageGroup().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((ImageGroup) domain).getName(), ((ImageGroup) domain).getProject().getName());
    }

    public Optional<ImageGroup> find(Long id) {
        Optional<ImageGroup> imageGroup = imageGroupRepository.findById(id);
        imageGroup.ifPresent(group -> securityACLService.check(group.container(), READ));
        return imageGroup;
    }

    public ImageGroup get(Long id) {
        return find(id).orElse(null);
    }

    public List<ImageGroup> list(Project project) {
        securityACLService.check(project, READ);

        List<ImageGroup> groups = imageGroupRepository.findAllByProject(project);
        for (ImageGroup group : groups) {
            group.setImages(imageGroupImageInstanceService.buildImageInstances(group));
        }

        return groups;
    }

    public CommandResponse add(JsonObject json) {
        transactionService.start();
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(json.getJSONAttrLong("project"), Project.class, READ);

        return executeCommand(new AddCommand(currentUser), null, json);
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), READ);

        return executeCommand(new EditCommand(currentUser, transaction), domain, jsonNewData);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), READ);

        return executeCommand(new DeleteCommand(currentUser, transaction), domain, null);
    }

    protected void beforeDelete(CytomineDomain domain) {
        ImageGroup imageGroup = (ImageGroup) domain;

        List<AnnotationGroup> annotationGroups = annotationGroupRepository.findAllByImageGroup(imageGroup);
        for (AnnotationGroup annotationGroup : annotationGroups) {
            annotationLinkRepository.deleteAllByGroup(annotationGroup);
        }

        annotationGroupRepository.deleteAllByImageGroup(imageGroup);
        imageGroupImageInstanceRepository.deleteAllByGroup(imageGroup);
    }
}
