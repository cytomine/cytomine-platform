package be.cytomine.controller.processing;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.processing.ImageFilterProjectService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.utils.JsonObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageFilterProjectController extends RestCytomineController {

    private final ImageFilterProjectService imageFilterProjectService;
    private final ProjectService projectService;

    @GetMapping("/imagefilterproject.json")
    public ResponseEntity<String> list(
    ) {
        log.debug("REST request to list imagefilterproject");
        return responseSuccess(imageFilterProjectService.list());
    }


    @GetMapping("/project/{id}/imagefilterproject.json")
    public ResponseEntity<String> listByProject(
            @PathVariable Long id
    ) {
        log.debug("REST request to list imagefilterproject for project {}", id);
        return projectService.find(id)
                .map( project -> responseSuccess(imageFilterProjectService.list(project)))
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
    }


    @PostMapping("/imagefilterproject.json")
    public ResponseEntity<String> add(@RequestBody String json) throws JsonProcessingException {
        log.debug("REST request to save imagefilterproject: " + json);
        return add(imageFilterProjectService, json);
    }

    @DeleteMapping("/imagefilterproject/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete imagefilterproject : " + id);
        return delete(imageFilterProjectService, JsonObject.of("id", id), null);
    }

}
