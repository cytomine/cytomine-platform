package be.cytomine.controller.image;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.NestedImageInstanceService;
import be.cytomine.service.image.UploadedFileService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestNestedImageInstanceController extends RestCytomineController {

    private final ImageInstanceService imageInstanceService;

    private final UploadedFileService uploadedFileService;

    private final NestedImageInstanceService nestedImageInstanceService;

    private final ImageServerService imageServerService;

    @GetMapping("/imageinstance/{imageInstanceId}/nested.json")
    public ResponseEntity<String> listByImageInstance(
            @PathVariable Long imageInstanceId
    ) {
        log.debug("REST request to list nested image for imageinstance {}", imageInstanceId);
        ImageInstance imageInstance = imageInstanceService.find(imageInstanceId)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", imageInstanceId));
        return responseSuccess(nestedImageInstanceService.list(imageInstance));
    }


    @GetMapping("/imageinstance/{imageInstanceId}/nested/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long imageInstanceId,
            @PathVariable Long id
    ) {
        log.debug("REST request to get nested image instance {}", id);
        return nestedImageInstanceService.find(id)
                .map(this::responseSuccess)
                .orElseThrow(() -> new ObjectNotFoundException("NestedImageInstance", id));
    }

    @PostMapping("/imageinstance/{imageInstanceId}/nested.json")
    public ResponseEntity<String> add(@PathVariable Long imageInstanceId, @RequestBody JsonObject json) {
        log.debug("REST request to save nested image instance : " + json);
        return add(nestedImageInstanceService, json);
    }

    @PutMapping("/imageinstance/{imageInstanceId}/nested/{id}.json")
    public ResponseEntity<String> edit(@PathVariable Long imageInstanceId, @PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit nested image instance : " + id);
        return update(nestedImageInstanceService, json);
    }

    @DeleteMapping("/imageinstance/{imageInstanceId}/nested/{id}.json")
    public ResponseEntity<String> delete(@PathVariable Long imageInstanceId, @PathVariable String id) {
        log.debug("REST request to delete nested image instance : " + id);
        return delete(nestedImageInstanceService, JsonObject.of("id", id), null);
    }
}
