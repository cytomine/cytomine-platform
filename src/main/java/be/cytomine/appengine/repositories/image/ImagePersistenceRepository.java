package be.cytomine.appengine.repositories.image;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.image.ImagePersistence;

@Repository
public interface ImagePersistenceRepository extends JpaRepository<ImagePersistence, UUID> {
    ImagePersistence findImagePersistenceByParameterNameAndRunIdAndParameterType(
        String parameterName,
        UUID run,
        ParameterType parameterType
    );
}
