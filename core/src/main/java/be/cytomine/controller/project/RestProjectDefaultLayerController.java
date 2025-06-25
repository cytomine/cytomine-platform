package be.cytomine.controller.project;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.project.ProjectDefaultLayerRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.project.ProjectDefaultLayerService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestProjectDefaultLayerController extends RestCytomineController {

    private final ProjectDefaultLayerService projectDefaultLayerService;

    private final ProjectDefaultLayerRepository projectDefaultLayerRepository;

    private final ProjectRepository projectRepository;

    private final TaskService taskService;

    private final ProjectService projectService;


    @GetMapping("/project/{id}/defaultlayer.json")
    public ResponseEntity<String> listByProject(
            @PathVariable Long id
    ) {
        log.debug("REST request to list projectDefaultLayers for project {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
        return responseSuccess(projectDefaultLayerService.listByProject(project));
    }

    @GetMapping("/project/{project}/defaultlayer/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable("project") Long projectId,
            @PathVariable Long id
    ) {
        log.debug("REST request to get ProjectDefaultLayer : {}", id);
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return projectDefaultLayerService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("ProjectDefaultLayer", id));
    }


    @PostMapping("/project/{id}/defaultlayer.json")
    public ResponseEntity<String> add(
            @PathVariable Long id,
            @RequestBody JsonObject json
    ) {
        log.debug("REST request to save ProjectDefaultLayer : " + json);
        return add(projectDefaultLayerService, json);
    }

    @PutMapping("/project/{project}/defaultlayer/{id}.json")
    public ResponseEntity<String> edit(
            @PathVariable("project") Long projectId,
            @PathVariable Long id,
            @RequestBody JsonObject json) {
        log.debug("REST request to edit ProjectDefaultLayer : " + id);
        return update(projectDefaultLayerService, json);
    }

    @DeleteMapping("/project/{project}/defaultlayer/{id}.json")
    public ResponseEntity<String> delete(
            @PathVariable("project") Long projectId,
            @PathVariable Long id,
            @RequestParam(required = false) Long task) {
        log.debug("REST request to delete ProjectDefaultLayer : " + id);
        Task existingTask = taskService.get(task);
        return delete(projectDefaultLayerService, JsonObject.of("id", id), existingTask);
    }

}
