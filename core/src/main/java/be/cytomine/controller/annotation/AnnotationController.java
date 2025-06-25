package be.cytomine.controller.annotation;

import java.io.IOException;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.cytomine.controller.ontology.RestAnnotationDomainController;
import be.cytomine.domain.annotation.Annotation;
import be.cytomine.service.annotation.AnnotationService;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class AnnotationController {

    private final AnnotationService annotationService;

    private final RestAnnotationDomainController annotationDomainController;

    @GetMapping("/annotations/{id}")
    public ResponseEntity<String> getById(@PathVariable Long id) throws IOException {
        log.info("Retrieve annotation {}", id);

        Optional<Annotation> annotation = annotationService.find(id);
        if (annotation.isPresent()) {
            return ResponseEntity.ok(annotation.get().toJSON());
        }

        // Retro-compatible method to get the annotation.
        return annotationDomainController.show(id);
    }
}
