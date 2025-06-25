package be.cytomine.appengine.repositories.number;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.number.NumberPersistence;

@Repository
public interface NumberPersistenceRepository extends JpaRepository<NumberPersistence, UUID> {
    NumberPersistence findNumberPersistenceByParameterNameAndRunIdAndParameterType(
        String parameterName,
        UUID run,
        ParameterType parameterType
    );
}
