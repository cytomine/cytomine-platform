package be.cytomine.appengine.repositories.collection;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.collection.CollectionPersistence;

@Repository
public interface CollectionPersistenceRepository
    extends JpaRepository<CollectionPersistence, UUID> {
    CollectionPersistence findCollectionPersistenceByParameterNameAndRunIdAndParameterType(
        String parameterName,
        UUID run,
        ParameterType parameterType
    );

    CollectionPersistence findCollectionPersistenceByParameterNameAndRunId(
        String parameterName,
        UUID run
    );
}
