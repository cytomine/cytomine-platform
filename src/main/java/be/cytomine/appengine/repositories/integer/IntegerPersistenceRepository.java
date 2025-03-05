package be.cytomine.appengine.repositories.integer;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.integer.IntegerPersistence;

@Repository
public interface IntegerPersistenceRepository extends JpaRepository<IntegerPersistence, UUID> {
    IntegerPersistence findIntegerPersistenceByParameterNameAndRunIdAndParameterType(
        String parameterName,
        UUID run,
        ParameterType parameterType
    );

    List<IntegerPersistence> findIntegerPersistenceByRunIdAndParameterType(
        UUID run,
        ParameterType parameterType
    );
}
