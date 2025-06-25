package be.cytomine.controller.meta;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.meta.TagRepository;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.meta.TagDomainAssociationService;
import be.cytomine.service.meta.TagService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.RequestParams;
import be.cytomine.utils.Task;
import be.cytomine.utils.filters.SearchParameterEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestTagDomainAssociationController extends RestCytomineController {

    private final TagService tagService;

    private final TagDomainAssociationService tagDomainAssociationService;

    private final TagRepository tagRepository;

    private final ProjectRepository projectRepository;

    private final TaskService taskService;

    private final AnnotationDomainRepository annotationDomainRepository;



    @GetMapping("/tag_domain_association/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get Tag domain association : {}", id);
        return tagDomainAssociationService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("Tag", id));
    }

    @GetMapping("/tag_domain_association.json")
    public ResponseEntity<String> list() {
        log.debug("REST request to list tags");
        List<SearchParameterEntry> searchParameterEntries = retrieveSearchParameters();
        RequestParams requestParams = retrievePageableParameters();
        return responseSuccess(tagDomainAssociationService.list(searchParameterEntries), requestParams.getOffset(), requestParams.getMax());
    }

    @GetMapping("/domain/{domainClassName}/{domainIdent}/tag_domain_association.json")
    public ResponseEntity<String> listByDomain(
            @PathVariable String domainClassName,
            @PathVariable Long domainIdent
    ) {
        log.debug("REST request to list tags for domain {} {}", domainClassName, domainIdent);
        CytomineDomain domain = null;
        if(domainClassName.contains("AnnotationDomain")) {
            domain = annotationDomainRepository.findById(domainIdent).orElse(null);
        } else {
            domain = tagDomainAssociationService.getCytomineDomain(domainClassName, domainIdent);
        }
        if (domain == null) {
            throw new ObjectNotFoundException(domainClassName, domainIdent);
        }
        return responseSuccess(tagDomainAssociationService.listAllByDomain(domain));
    }

    @PostMapping({"/tag_domain_association.json", "/domain/{domainClassName}/{domainIdent}/tag_domain_association.json"})
    public ResponseEntity<String> add(
            @PathVariable(required = false) String domainClassName,
            @PathVariable(required = false) Long domainIdent,
            @RequestBody JsonObject json) {
        log.debug("REST request to save Tag association: " + json);
        return add(tagDomainAssociationService, json);
    }

    @DeleteMapping("/tag_domain_association/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id, @RequestParam(required = false) Long task) {
        log.debug("REST request to delete Tag association: " + id);
        Task existingTask = taskService.get(task);
        return delete(tagDomainAssociationService, JsonObject.of("id", id), existingTask);
    }
}
