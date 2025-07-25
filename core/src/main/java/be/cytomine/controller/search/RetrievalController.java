package be.cytomine.controller.search;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.dto.search.SearchResponse;
import be.cytomine.service.search.RetrievalService;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class RetrievalController {

    private final EntityManager entityManager;

    private final RetrievalService retrievalService;

    @GetMapping("/retrieval/index/{id}")
    public ResponseEntity<String> indexAnnotation(@PathVariable Long id) {
        log.debug("Create index for annotation {}", id);

        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(entityManager, id);

        return retrievalService.indexAnnotation(annotation);
    }

    @GetMapping("/retrieval/search")
    public ResponseEntity<SearchResponse> retrieveSimilarAnnotations(
        @RequestParam(value = "annotation") Long id,
        @RequestParam(value = "nrt_neigh") Long nrt_neigh
    ) {
        log.debug("Retrieve similar annotations for query annotation {}", id);

        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(entityManager, id);

        return retrievalService.retrieveSimilarImages(annotation, nrt_neigh);
    }
}