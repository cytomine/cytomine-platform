package be.cytomine.appengine.repositories.bool;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.bool.BooleanPersistence;

@Repository
public interface BooleanPersistenceRepository extends JpaRepository<BooleanPersistence, UUID> {
    BooleanPersistence findBooleanPersistenceByParameterNameAndRunIdAndParameterType(String parameterName, UUID run , ParameterType parameterType);
}
