package be.cytomine.controller.ontology;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestOntologyController extends RestCytomineController {

    private final OntologyService ontologyService;

    private final OntologyRepository ontologyRepository;

    private final ProjectRepository projectRepository;

    private final TaskService taskService;

    /**
     * List all ontology visible for the current user
     * For each ontology, print the terms tree
     */
    @GetMapping("/ontology.json")
    public ResponseEntity<String> list(
            @RequestParam Map<String,String> allParams
    ) {
        log.debug("REST request to list ontologys");
        boolean light = allParams.containsKey("light") && Boolean.parseBoolean(allParams.get("light"));
        return responseSuccess(light ? ontologyService.listLight() : ontologyService.list());
    }

    @GetMapping("/ontology/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get Ontology : {}", id);
        return ontologyService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("Ontology", id));
    }


    @PostMapping("/ontology.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save Ontology : " + json);
        return add(ontologyService, json);
    }

    @PutMapping("/ontology/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit Ontology : " + id);
        return update(ontologyService, json);
    }

    @DeleteMapping("/ontology/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id, @RequestParam(required = false) Long task) {
        log.debug("REST request to delete Ontology : " + id);
        Task existingTask = taskService.get(task);
        return delete(ontologyService, JsonObject.of("id", id), existingTask);
    }

}
