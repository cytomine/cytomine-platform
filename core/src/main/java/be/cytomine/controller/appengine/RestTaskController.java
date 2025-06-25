package be.cytomine.controller.appengine;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.service.appengine.AppEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@RestController
@RequestMapping("/api/app-engine") // Defined an "app-engine" prefix to avoid clash with existing Task Controller.
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("${application.appEngine.enabled: false}")
public class RestTaskController extends RestCytomineController {

    @Autowired
    private AppEngineService appEngineService;

    @GetMapping("/tasks/{id}")
    public ResponseEntity<String> descriptionById(
            @PathVariable String id
    ) {
        return appEngineService.get("tasks/" + id);
    }

    @GetMapping("/tasks/{namespace}/{version}")
    public ResponseEntity<String> description(
            @PathVariable String namespace,
            @PathVariable String version
    ) {
        return appEngineService.get("tasks/" + namespace + "/" + version);
    }

    @GetMapping("/tasks")
    public ResponseEntity<String> list() {
        return appEngineService.get("tasks");
    }

    @PostMapping("/tasks")
    public ResponseEntity<String> upload(
            @RequestParam("task") MultipartFile task
    ) throws IOException {
        String name = UUID.randomUUID().toString();
        File tmpFile = Files.createTempFile(name, null).toFile();
        task.transferTo(tmpFile);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("task", new FileSystemResource(tmpFile));
        return appEngineService.post("tasks", body, MediaType.MULTIPART_FORM_DATA);
    }

    @GetMapping("/tasks/{id}/inputs")
    public ResponseEntity<String> inputsById(
            @PathVariable UUID id
    ) {
        return appEngineService.get("tasks/" + id + "/inputs");
    }
    
    @GetMapping("/tasks/{namespace}/{version}/inputs")
    public ResponseEntity<String> inputs(
            @PathVariable String namespace,
            @PathVariable String version
    ) {
        return appEngineService.get("tasks/" + namespace + "/" + version + "/inputs");
    }

    @GetMapping("/tasks/{id}/outputs")
    public ResponseEntity<String> outputsById(
            @PathVariable UUID id
    ) {
        return appEngineService.get("tasks/" + id + "/outputs");
    }

    @GetMapping("/tasks/{namespace}/{version}/outputs")
    public ResponseEntity<String> outputs(
            @PathVariable String namespace,
            @PathVariable String version
    ) {
        return appEngineService.get("tasks/" + namespace + "/" + version + "/outputs");
    }

    @GetMapping("/tasks/{id}/descriptor.yml")
    public ResponseEntity<String> descriptorById(
            @PathVariable UUID id
    ) {
        return appEngineService.get("tasks/" + id + "/descriptor.yml");
    }

    @GetMapping("/tasks/{namespace}/{version}/descriptor.yml")
    public ResponseEntity<String> descriptor(
            @PathVariable String namespace,
            @PathVariable String version
    ) {
        return appEngineService.get("tasks/" + namespace + "/" + version + "/descriptor.yml");
    }
}
