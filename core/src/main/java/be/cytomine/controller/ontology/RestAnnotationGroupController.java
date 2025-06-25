package be.cytomine.controller.ontology;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.group.ImageGroupService;
import be.cytomine.service.ontology.AnnotationGroupService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAnnotationGroupController extends RestCytomineController {

    private final AnnotationGroupService annotationGroupService;

    private final ImageGroupService imageGroupService;

    private final ProjectService projectService;

    @PostMapping("/annotationgroup.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save annotationgroup: " + json);
        return add(annotationGroupService, json);
    }

    @GetMapping("/annotationgroup/{id}.json")
    public ResponseEntity<String> show(@PathVariable Long id) {
        log.debug("REST request to show annotationgroup: " + id);
        return annotationGroupService.find(id)
                .map(this::responseSuccess)
                .orElseThrow(() -> new ObjectNotFoundException("AnnotationGroup", id));
    }

    @PutMapping("/annotationgroup/{id}.json")
    public ResponseEntity<String> edit(@PathVariable Long id, @RequestBody JsonObject json) {
        log.debug("REST request to edit annotationgroup: " + id);
        return update(annotationGroupService, json);
    }

    @DeleteMapping("/annotationgroup/{id}.json")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        log.debug("REST request to delete annotationgroup: " + id);
        return delete(annotationGroupService, JsonObject.of("id", id), null);
    }

    @GetMapping("/project/{id}/annotationgroup.json")
    public ResponseEntity<String> listByProject(@PathVariable Long id) {
        log.debug("REST request to list annotationgroup for project " + id);
        Project project = projectService.find(id).orElseThrow(() -> new ObjectNotFoundException("Project", id));
        return responseSuccess(annotationGroupService.list(project));
    }

    @GetMapping("/imagegroup/{id}/annotationgroup.json")
    public ResponseEntity<String> listByImageGroup(@PathVariable Long id) {
        log.debug("REST request to list annotationgroup for imagegroup " + id);
        ImageGroup group = imageGroupService.find(id).orElseThrow(() -> new ObjectNotFoundException("ImageGroup", id));
        return responseSuccess(annotationGroupService.list(group));
    }

    @PostMapping("/annotationgroup/{id}/annotationgroup/{mergedId}/merge.json")
    public ResponseEntity<String> merge(@PathVariable Long id, @PathVariable Long mergedId) {
        log.debug("REST request to merge annotationgroup {} with annotationgroup {}", id, mergedId);
        return responseSuccess(annotationGroupService.merge(id, mergedId));
    }
}
