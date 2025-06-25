package be.cytomine.service.ontology;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.AddCommand;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.ontology.AnnotationLink;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
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
public class AnnotationLinkService extends ModelService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AnnotationGroupService annotationGroupService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AnnotationLinkRepository annotationLinkRepository;

    @Override
    public Class currentDomain() {
        return AnnotationLink.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new AnnotationLink().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((AnnotationLink) domain).getGroup().getId());
    }

    public Optional<AnnotationLink> find(Long id) {
        Optional<AnnotationLink> annotationLink = annotationLinkRepository.findById(id);
        annotationLink.ifPresent(group -> securityACLService.check(group.container(), READ));
        return annotationLink;
    }

    public Optional<AnnotationLink> find(AnnotationGroup group, AnnotationDomain annotation) {
        Optional<AnnotationLink> annotationLink = annotationLinkRepository.findByAnnotationIdentAndGroup(annotation.getId(), group);
        annotationLink.ifPresent(link -> securityACLService.check(link.container(), READ));
        return annotationLink;
    }

    public AnnotationLink get(Long id) {
        return find(id).orElse(null);
    }

    public AnnotationLink get(AnnotationGroup group, AnnotationDomain annotation) {
        return find(group, annotation).orElse(null);
    }

    public List<AnnotationLink> list(AnnotationGroup group) {
        securityACLService.check(group, READ);
        return annotationLinkRepository.findAllByGroup(group);
    }

    public CommandResponse add(JsonObject json) {
        transactionService.start();
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(entityManager, json.getJSONAttrLong("annotationIdent"));
        securityACLService.check(annotation.getProject(), READ);
        securityACLService.checkIsNotReadOnly(annotation.getProject());

        AnnotationGroup group = annotationGroupService.get(json.getJSONAttrLong("group"));
        if (group.getProject() != annotation.getProject()) {
            throw new WrongArgumentException("Group and annotation are not in the same project!");
        }

        json.put("annotationIdent", annotation.getId());
        json.put("annotationClassName", annotation.getClass().getName());

        return executeCommand(new AddCommand(currentUser), null, json);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), READ);

        return executeCommand(new DeleteCommand(currentUser, transaction), domain, null);
    }

    public CommandResponse addAnnotationLink(String annotationClassName, Long annotationIdent, Long groupId, Long imageId, Transaction transaction) {
        JsonObject jsonObject = JsonObject.of(
                "annotationClassName", annotationClassName,
                "annotationIdent", annotationIdent,
                "group", groupId,
                "image", imageId
        );

        return executeCommand(new AddCommand(currentUserService.getCurrentUser(), transaction), null, jsonObject);
    }
}
