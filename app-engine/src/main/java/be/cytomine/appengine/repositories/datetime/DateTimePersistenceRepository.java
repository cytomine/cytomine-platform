package be.cytomine.appengine.repositories.datetime;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.datetime.DateTimePersistence;

@Repository
public interface DateTimePersistenceRepository extends JpaRepository<DateTimePersistence, UUID> {
    DateTimePersistence findDateTimePersistenceByParameterNameAndRunIdAndParameterType(
        String parameterName,
        UUID run,
        ParameterType parameterType
    );
}
