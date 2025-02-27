package be.cytomine.appengine.repositories.collection;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.bool.BooleanPersistence;
import java.util.UUID;

import be.cytomine.appengine.models.task.collection.CollectionPersistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionPersistenceRepository extends JpaRepository<CollectionPersistence, UUID> {
    CollectionPersistence findCollectionPersistenceByParameterNameAndRunIdAndParameterType(
        String parameterName,
        UUID run,
        ParameterType parameterType
    );
}
