package be.cytomine.repository.annotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.domain.annotation.AnnotationLayer;

@Repository
public interface AnnotationLayerRepository extends JpaRepository<AnnotationLayer, Long> {}
