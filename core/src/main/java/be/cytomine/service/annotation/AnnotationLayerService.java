package be.cytomine.service.annotation;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import be.cytomine.domain.annotation.Annotation;
import be.cytomine.domain.annotation.AnnotationLayer;
import be.cytomine.domain.appengine.TaskRunLayer;
import be.cytomine.dto.appengine.task.TaskRunLayerValue;
import be.cytomine.repository.annotation.AnnotationLayerRepository;
import be.cytomine.repository.annotation.AnnotationRepository;
import be.cytomine.repository.appengine.TaskRunLayerRepository;
import be.cytomine.service.appengine.TaskRunLayerService;

@Service
@RequiredArgsConstructor
public class AnnotationLayerService {

    private final AnnotationRepository annotationRepository;

    private final AnnotationLayerRepository annotationLayerRepository;

    private final TaskRunLayerRepository taskRunLayerRepository;

    private final TaskRunLayerService taskRunLayerService;

    public AnnotationLayer createAnnotationLayer(String name) {
        AnnotationLayer annotationLayer = new AnnotationLayer();
        annotationLayer.setName(name);

        return annotationLayerRepository.saveAndFlush(annotationLayer);
    }

    public Optional<AnnotationLayer> find(Long id) {
        return annotationLayerRepository.findById(id);
    }

    public List<AnnotationLayer> findByTaskRunLayer(Long imageId) {
        List<TaskRunLayer> taskRunLayers = taskRunLayerRepository.findAllByImageId(imageId);

        return taskRunLayers
            .stream()
            .map(TaskRunLayer::getAnnotationLayer)
            .toList();
    }

    public TaskRunLayerValue findTaskRunLayer(Long id) {
        Optional<TaskRunLayer> optional = taskRunLayerRepository.findByAnnotationLayerId(id);
        if (optional.isEmpty()) {
            return null;
        }

        return taskRunLayerService.convertToDTO(optional.get());
    }

    public List<Annotation> findAnnotationsByLayer(AnnotationLayer layer) {
        return annotationRepository.findAllByAnnotationLayer(layer);
    }
}
