package be.cytomine.controller.ontology;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.ontology.TrackService;
import be.cytomine.utils.JsonObject;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class RestTrackController extends RestCytomineController {

    private final TrackService trackService;

    private final ImageInstanceRepository imageInstanceRepository;

    private final ProjectRepository projectRepository;

    @GetMapping("/project/{id}/track.json")
    public ResponseEntity<String> listByProject(
            @PathVariable Long id
    ) {
        log.debug("REST request to list tracks for project {}", id);
        return projectRepository.findById(id)
                .map( project -> responseSuccess(trackService.list(project)))
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
    }

    @GetMapping("/imageinstance/{id}/track.json")
    public ResponseEntity<String> listByImage(
            @PathVariable Long id
    ) {
        log.debug("REST request to list tracks for imageinstance {}", id);
        return imageInstanceRepository.findById(id)
                .map( imageInstance -> responseSuccess(trackService.list(imageInstance)))
                .orElseThrow(() -> new ObjectNotFoundException("imageInstance", id));
    }

    @GetMapping("/track/{id}.json")
    public ResponseEntity<String> show(@PathVariable Long id) {
        log.debug("REST request to get Track : {}", id);
        return trackService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("Track", id));
    }

    /**
     * Add a new track
     * @param json JSON with Track data
     * @return Response map with .code = http response code and .data.track = new created Track
     */
    @PostMapping("/track.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save Track : " + json);
        return add(trackService, json);
    }

    /**
     * Update a track
     * @param id Track id
     * @param json JSON with the new Track data
     * @return Response map with .code = http response code and .data.newTrack = new created Track and  .data.oldTrack = old track value
     */
    @PutMapping("/track/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit Track : " + id);
        return update(trackService, json);
    }

    /**
     * Delete a track
     * @param id Track id
     * @return Response map with .code = http response code and .data.track = deleted track value
     */
    @DeleteMapping("/track/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete Track : " + id);
        return delete(trackService, JsonObject.of("id", id), null);
    }
}
