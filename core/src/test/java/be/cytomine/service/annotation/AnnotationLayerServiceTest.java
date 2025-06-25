package be.cytomine.service.annotation;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import be.cytomine.domain.annotation.Annotation;
import be.cytomine.domain.annotation.AnnotationLayer;
import be.cytomine.domain.appengine.TaskRun;
import be.cytomine.domain.appengine.TaskRunLayer;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.dto.appengine.task.TaskRunLayerValue;
import be.cytomine.repository.annotation.AnnotationLayerRepository;
import be.cytomine.repository.annotation.AnnotationRepository;
import be.cytomine.repository.appengine.TaskRunLayerRepository;
import be.cytomine.service.appengine.TaskRunLayerService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnnotationLayerServiceTest {

    @Mock
    private AnnotationRepository annotationRepository;

    @Mock
    private AnnotationLayerRepository annotationLayerRepository;

    @Mock
    private TaskRunLayerRepository taskRunLayerRepository;

    @Mock
    private TaskRunLayerService taskRunLayerService;

    @InjectMocks
    private AnnotationLayerService annotationLayerService;

    private static Annotation mockAnnotation;

    private static AnnotationLayer mockAnnotationLayer;

    private static ImageInstance mockImage;

    private static TaskRun mockTaskRun;

    private static TaskRunLayer mockTaskRunLayer;

    private static String name;

    @BeforeAll
    public static void setUp() {
        name = UUID.randomUUID().toString();
        mockAnnotationLayer = new AnnotationLayer();
        mockAnnotationLayer.setName(name);

        String mockGeometry = "{\"type\": \"Point\",\"coordinates\": [0, 0]}";
        mockAnnotation = new Annotation();
        mockAnnotation.setAnnotationLayer(mockAnnotationLayer);
        mockAnnotation.setLocation(mockGeometry.getBytes());

        mockImage = new ImageInstance();

        mockTaskRun = new TaskRun();
        mockTaskRun.setImage(mockImage);

        mockTaskRunLayer = new TaskRunLayer();
        mockTaskRunLayer.setAnnotationLayer(mockAnnotationLayer);
        mockTaskRunLayer.setTaskRun(mockTaskRun);
        mockTaskRunLayer.setImage(mockImage);
        mockTaskRunLayer.setXOffset(new Random().nextInt(100));
        mockTaskRunLayer.setYOffset(new Random().nextInt(100));
    }

    @Test
    public void createAnnotationLayerShouldReturnAnnotationLayer() {
        when(annotationLayerRepository.saveAndFlush(any(AnnotationLayer.class))).thenReturn(mockAnnotationLayer);

        AnnotationLayer result = annotationLayerService.createAnnotationLayer(name);
    
        assertNotNull(result);
        assertEquals(mockAnnotationLayer.getId(), result.getId());
        assertEquals(mockAnnotationLayer.getName(), result.getName());

        verify(annotationLayerRepository, times(1)).saveAndFlush(any(AnnotationLayer.class));
    }

    @Test
    public void findShouldReturnAnnotationLayer() {
        when(annotationLayerRepository.findById(mockAnnotationLayer.getId())).thenReturn(Optional.of(mockAnnotationLayer));

        Optional<AnnotationLayer> result = annotationLayerService.find(mockAnnotationLayer.getId());

        assertTrue(result.isPresent());
        assertEquals(mockAnnotationLayer, result.get());

        verify(annotationLayerRepository, times(1)).findById(mockAnnotationLayer.getId());
    }

    @Test
    public void findShouldReturnEmpty() {
        when(annotationLayerRepository.findById(mockAnnotationLayer.getId())).thenReturn(Optional.empty());

        Optional<AnnotationLayer> result = annotationLayerService.find(mockAnnotationLayer.getId());

        assertTrue(result.isEmpty());

        verify(annotationLayerRepository, times(1)).findById(mockAnnotationLayer.getId());
    }

    @Test
    public void findByTaskRunLayerShouldReturnAnnotationLayers() {
        List<TaskRunLayer> mockTaskRunLayers = List.of(mockTaskRunLayer, mockTaskRunLayer);
        when(taskRunLayerRepository.findAllByImageId(mockImage.getId())).thenReturn(mockTaskRunLayers);

        List<AnnotationLayer> results = annotationLayerService.findByTaskRunLayer(mockImage.getId());

        assertFalse(results.isEmpty());
        assertEquals(mockTaskRunLayers.size(), results.size());

        verify(taskRunLayerRepository, times(1)).findAllByImageId(mockImage.getId());
    }

    @Test
    public void findTaskRunLayerShouldReturnTaskRunLayerValue() {
        TaskRunLayerValue expected = new TaskRunLayerValue(
            mockTaskRunLayer.getAnnotationLayer().getId(),
            mockTaskRunLayer.getTaskRun().getId(),
            mockTaskRunLayer.getImage().getId(),
            mockTaskRunLayer.getXOffset(),
            mockTaskRunLayer.getYOffset()
        );
        when(taskRunLayerRepository.findByAnnotationLayerId(mockAnnotationLayer.getId())).thenReturn(Optional.of(mockTaskRunLayer));
        when(taskRunLayerService.convertToDTO(mockTaskRunLayer)).thenReturn(expected);

        TaskRunLayerValue result = annotationLayerService.findTaskRunLayer(mockAnnotationLayer.getId());

        assertNotNull(result);
        assertEquals(result, expected);

        verify(taskRunLayerRepository, times(1)).findByAnnotationLayerId(mockAnnotationLayer.getId());
        verify(taskRunLayerService, times(1)).convertToDTO(mockTaskRunLayer);
    }

    @Test
    public void findTaskRunLayerShouldReturnNull() {
        when(taskRunLayerRepository.findByAnnotationLayerId(mockAnnotationLayer.getId())).thenReturn(Optional.empty());

        TaskRunLayerValue result = annotationLayerService.findTaskRunLayer(mockAnnotationLayer.getId());

        assertNull(result);

        verify(taskRunLayerRepository, times(1)).findByAnnotationLayerId(mockAnnotationLayer.getId());
    }

    @Test
    public void findAnnotationsByLayerShouldReturnAnnotations() {
        List<Annotation> mockAnnotations = List.of(mockAnnotation, mockAnnotation);
        when(annotationRepository.findAllByAnnotationLayer(mockAnnotationLayer)).thenReturn(mockAnnotations);

        List<Annotation> results = annotationLayerService.findAnnotationsByLayer(mockAnnotationLayer);

        assertFalse(results.isEmpty());
        assertEquals(mockAnnotations.size(), results.size());

        verify(annotationRepository, times(1)).findAllByAnnotationLayer(mockAnnotationLayer);
    }
}
