package be.cytomine.service.image.group;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.AddCommand;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.image.group.ImageGroupImageInstance;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.group.ImageGroupImageInstanceRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class ImageGroupImageInstanceService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ImageGroupService imageGroupService;

    @Autowired
    private ImageInstanceService imageInstanceService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ImageGroupImageInstanceRepository imageGroupImageInstanceRepository;

    @Override
    public Class currentDomain() {
        return ImageGroupImageInstance.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new ImageGroupImageInstance().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(
                domain.getId(),
                ((ImageGroupImageInstance) domain).getGroup().getName(),
                ((ImageGroupImageInstance) domain).getImage().getBlindInstanceFilename()
        );
    }

    public Optional<ImageGroupImageInstance> find(ImageGroup group, ImageInstance image) {
        Optional<ImageGroupImageInstance> igii = imageGroupImageInstanceRepository.findByGroupAndImage(group, image);
        igii.ifPresent(img -> securityACLService.check(img.container(), READ));
        return igii;
    }

    public ImageGroupImageInstance get(ImageGroup group, ImageInstance image) {
        return find(group, image).orElse(null);
    }

    public ImageGroupImageInstance retrieve(JsonObject json) {
        ImageGroup group = imageGroupService.get(json.getJSONAttrLong("group", null));
        ImageInstance image = imageInstanceService.get(json.getJSONAttrLong("image", null));

        if (group.getProject() != image.getProject()) {
            throw new WrongArgumentException("Group and image are not in the same project!");
        }

        return get(group, image);
    }

    public List<ImageGroupImageInstance> list(ImageInstance image) {
        securityACLService.check(image.container(), READ);
        return imageGroupImageInstanceRepository.findAllByImage(image);
    }

    public List<ImageGroupImageInstance> list(ImageGroup group) {
        securityACLService.check(group.container(), READ);
        return imageGroupImageInstanceRepository.findAllByGroup(group);
    }

    public CommandResponse add(JsonObject json) {
        ImageGroup group = imageGroupService.get(json.getJSONAttrLong("group", null));
        ImageInstance image = imageInstanceService.get(json.getJSONAttrLong("image", null));

        if (group.getProject() != image.getProject()) {
            throw new WrongArgumentException("Group and image are not in the same project!");
        }

        transactionService.start();
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.checkIsNotReadOnly(group.getProject());

        return executeCommand(new AddCommand(currentUser), null, json);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), READ);

        return executeCommand(new DeleteCommand(currentUser, transaction), domain, null);
    }

    public List<ImageInstance> getImages(Long groupId, Long imageId) {
        ImageGroup group = imageGroupService.get(groupId);
        ImageInstance image = imageInstanceService.get(imageId);

        if (group == null || image == null || get(group, image) == null) {
            return null;
        }

        return imageGroupImageInstanceRepository.findAllByGroup(group)
                .stream()
                .map((ImageGroupImageInstance::getImage))
                .sorted(Comparator.comparing(ImageInstance::getBlindInstanceFilename))
                .toList();
    }

    public List<Object> buildImageInstances(ImageGroup group) {
        List<Object> images = new ArrayList<>();
        for (ImageGroupImageInstance igii : list(group)) {
            images.add(Map.of(
                    "id", igii.getImage().getId(),
                    "instanceFilename", igii.getImage().getBlindInstanceFilename(),
                    "thumb", UrlApi.getImageInstanceThumbUrlWithMaxSize(igii.getImage().getId()),
                    "width", igii.getImage().getBaseImage().getWidth(),
                    "height", igii.getImage().getBaseImage().getHeight()
            ));
        }

        return images;
    }
}
