package be.cytomine.controller.project;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.project.ProjectRepresentativeUserRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.project.ProjectRepresentativeUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.UserService;
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
public class RestProjectRepresentativeUserController extends RestCytomineController {

    private final ProjectRepresentativeUserService projectRepresentativeUserService;

    private final ProjectRepresentativeUserRepository projectRepresentativeUserRepository;

    private final ProjectRepository projectRepository;

    private final TaskService taskService;

    private final ProjectService projectService;

    private final UserService userService;


    @GetMapping("/project/{id}/representative.json")
    public ResponseEntity<String> listByProject(
            @PathVariable Long id
    ) {
        log.debug("REST request to list projectRepresentativeUsers for project {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
        return responseSuccess(projectRepresentativeUserService.listByProject(project));
    }

    @GetMapping("/project/{project}/representative/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable("project") Long projectId,
            @PathVariable Long id
    ) {
        log.debug("REST request to get ProjectRepresentativeUser : {}", id);
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return projectRepresentativeUserService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("ProjectRepresentativeUser", id));
    }
    
    @PostMapping("/project/{id}/representative.json")
    public ResponseEntity<String> add(
            @PathVariable Long id,
            @RequestBody JsonObject json
    ) {
        log.debug("REST request to save ProjectRepresentativeUser : " + json);
        return add(projectRepresentativeUserService, json);
    }

    @DeleteMapping({"/project/{project}/representative/{id}.json", "/project/{project}/representative.json"})
    public ResponseEntity<String> delete(
            @PathVariable(value = "project", required = true) Long projectId,
            @PathVariable(required = false) Long id,
            @RequestParam(value = "user", required = false) Long userId,
            @RequestParam(required = false) Long task) {
        log.debug("REST request to delete ProjectRepresentativeUser");

        ProjectRepresentativeUser projectRepresentativeUser;
        if(id!=null) {
            projectRepresentativeUser = projectRepresentativeUserService.find(id)
                    .orElseThrow(() -> new ObjectNotFoundException("ProjectRepresentativeUser", id));
        } else {
            Project project = projectService.find(projectId)
                    .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
            User user = userService.findUser(userId)
                    .orElseThrow(() -> new ObjectNotFoundException("User", userId));
            projectRepresentativeUser = projectRepresentativeUserService.find(project, user)
                    .orElseThrow(() -> new ObjectNotFoundException("ProjectRepresentativeUser", JsonObject.of("project", projectId, "user", userId).toJsonString()));
        }
        Task existingTask = taskService.get(task);
        return delete(projectRepresentativeUserService, JsonObject.of("id", projectRepresentativeUser.getId()), existingTask);
    }

}
