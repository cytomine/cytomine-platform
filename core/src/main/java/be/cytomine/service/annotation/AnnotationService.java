package be.cytomine.service.annotation;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import be.cytomine.domain.annotation.Annotation;
import be.cytomine.domain.annotation.AnnotationLayer;
import be.cytomine.repository.annotation.AnnotationRepository;

@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationRepository annotationRepository;

    public Annotation createAnnotation(AnnotationLayer layer, String geometry) {
        Annotation annotation = new Annotation();
        annotation.setAnnotationLayer(layer);
        annotation.setLocation(geometry.getBytes(StandardCharsets.UTF_8));

        return annotationRepository.saveAndFlush(annotation);
    }

    public Optional<Annotation> find(Long id) {
        return annotationRepository.findById(id);
    }
}
