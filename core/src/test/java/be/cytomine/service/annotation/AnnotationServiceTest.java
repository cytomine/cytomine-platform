package be.cytomine.service.annotation;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import be.cytomine.domain.annotation.Annotation;
import be.cytomine.domain.annotation.AnnotationLayer;
import be.cytomine.repository.annotation.AnnotationRepository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnnotationServiceTest {

    @Mock
    private AnnotationRepository annotationRepository;

    @InjectMocks
    private AnnotationService annotationService;

    private static Annotation mockAnnotation;

    private static AnnotationLayer mockAnnotationLayer;

    private static String mockGeometry;

    @BeforeAll
    public static void setUp() {
        mockAnnotationLayer = new AnnotationLayer();

        mockGeometry = "{\"type\": \"Point\",\"coordinates\": [0, 0]}";
        mockAnnotation = new Annotation();
        mockAnnotation.setAnnotationLayer(mockAnnotationLayer);
        mockAnnotation.setLocation(mockGeometry.getBytes());
    }

    @DisplayName("Successfully create an annotation")
    @Test
    public void createAnnotationShouldReturnAnnotation() {
        when(annotationRepository.saveAndFlush(any(Annotation.class))).thenReturn(mockAnnotation);
    
        Annotation result = annotationService.createAnnotation(mockAnnotationLayer, mockGeometry);
    
        assertNotNull(result);
        assertEquals(mockAnnotation.getId(), result.getId());
        assertEquals(mockAnnotation.getAnnotationLayer(), result.getAnnotationLayer());
        assertArrayEquals(mockAnnotation.getLocation(), result.getLocation());

        verify(annotationRepository, times(1)).saveAndFlush(any(Annotation.class));
    }

    @DisplayName("Successfully find an annotation given an ID")
    @Test
    public void findShouldReturnAnnotation() {
        when(annotationRepository.findById(1L)).thenReturn(Optional.of(mockAnnotation));

        Optional<Annotation> result = annotationService.find(1L);

        assertTrue(result.isPresent());
    }

    @DisplayName("Successfully find an empty annotation given an unexisting ID")
    @Test
    public void findShouldReturnEmptyAnnotation() {
        when(annotationRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Annotation> result = annotationService.find(1L);

        assertTrue(result.isEmpty());
    }
}
