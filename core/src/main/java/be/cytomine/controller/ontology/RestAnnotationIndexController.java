package be.cytomine.controller.ontology;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.ontology.AnnotationIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAnnotationIndexController extends RestCytomineController {

    private final AnnotationIndexService annotationIndexService;

    private final SliceInstanceService sliceInstanceService;
    /**
     * List all ontology visible for the current user
     * For each ontology, print the terms tree
     */
    @GetMapping("/sliceinstance/{id}/annotationindex.json")
    public ResponseEntity<String> listBySlice(
            @PathVariable Long id
    ) {
        log.debug("REST request to get annotationindex for slice {}", id);
        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id));
        return responseSuccess(annotationIndexService.list(sliceInstance));
    }

}
