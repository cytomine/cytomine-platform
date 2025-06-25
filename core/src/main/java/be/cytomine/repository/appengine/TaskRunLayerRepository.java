package be.cytomine.repository.appengine;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.domain.appengine.TaskRunLayer;

@Repository
public interface TaskRunLayerRepository extends JpaRepository<TaskRunLayer, Long> {
    List<TaskRunLayer> findAllByImageId(Long imageId);

    Optional<TaskRunLayer> findByAnnotationLayerId(Long id);
}
