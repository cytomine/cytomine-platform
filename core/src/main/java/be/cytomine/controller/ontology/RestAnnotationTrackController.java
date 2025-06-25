package be.cytomine.controller.ontology;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.Track;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.ontology.AnnotationTrackService;
import be.cytomine.service.ontology.TrackService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAnnotationTrackController extends RestCytomineController {

    private final AnnotationTrackService annotationTrackService;

    private final TrackService trackService;

    private final AnnotationDomainRepository annotationDomainRepository;

    private final ProjectRepository projectRepository;

    @GetMapping("/track/{id}/annotationtrack.json")
    public ResponseEntity<String> listByTrack(
            @PathVariable Long id
    ) {
        log.debug("REST request to list annotationTracks for track {}", id);
        return responseSuccess(annotationTrackService.list(trackService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Track", id))));
    }

    @GetMapping("/annotation/{id}/annotationtrack.json")
    public ResponseEntity<String> listByAnnotation(
            @PathVariable Long id
    ) {
        log.debug("REST request to list annotationTracks for annotation {}", id);
        return responseSuccess(annotationTrackService.list(annotationDomainRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", id))));
    }


    @GetMapping("/annotationtrack/{annotation}/{track}.json")
    public ResponseEntity<String> show(
            @PathVariable(value = "annotation") Long annotationId,
            @PathVariable(value = "track") Long trackId
            ) {
        log.debug("REST request to get AnnotationTrack : {} {}", annotationId, trackId);
        AnnotationDomain annotationDomain = annotationDomainRepository.findById(annotationId)
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", annotationId));
        Track track = trackService.find(trackId)
                .orElseThrow(() -> new ObjectNotFoundException("Track", trackId));

        return annotationTrackService.find(annotationDomain, track)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("AnnotationTrack", Map.of("annotation", annotationId, "track", trackId)));
    }

    @DeleteMapping("/annotationtrack/{annotation}/{track}.json")
    public ResponseEntity<String> delete(
            @PathVariable(value = "annotation") Long annotationId,
            @PathVariable(value = "track") Long trackId
    ) {
        log.debug("REST request to get AnnotationTrack : {} {}", annotationId, trackId);
        return delete(annotationTrackService, JsonObject.of("annotationIdent", annotationId, "track", trackId), null);
    }

    @PostMapping("/annotationtrack.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save AnnotationTrack : " + json);
        return add(annotationTrackService, json);
    }

}
