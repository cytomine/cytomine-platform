package be.cytomine.controller.image.server;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.image.server.StorageService;
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
public class RestStorageController extends RestCytomineController {

    private final StorageService storageService;

    private final CurrentUserService currentUserService;

    private final TaskService taskService;

    /**
     * List all storage visible for the current user
     * For each storage, print the terms tree
     */
    @GetMapping("/storage.json")
    public ResponseEntity<String> list(
            @RequestParam(defaultValue = "false", required = false) Boolean all
    ) {
        log.debug("REST request to list storages: all? {}", all);
        return responseSuccess(all ? storageService.list() : storageService.list(currentUserService.getCurrentUser(), null));
    }

    @GetMapping("/storage/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get Storage : {}", id);
        return storageService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("Storage", id));
    }


    @PostMapping("/storage.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save Storage : " + json);
        return add(storageService, json);
    }

    @PutMapping("/storage/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit Storage : " + id);
        return update(storageService, json);
    }

    @DeleteMapping("/storage/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id, @RequestParam(required = false) Long task) {
        log.debug("REST request to delete Storage : " + id);
        Task existingTask = taskService.get(task);
        return delete(storageService, JsonObject.of("id", id), existingTask);
    }

}
