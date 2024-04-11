package be.cytomine.appengine.repositories;



import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.TypePersistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TypePersistenceRepository extends JpaRepository<TypePersistence, UUID> {
    TypePersistence findTypePersistenceByParameterNameAndRunId(String parameterName, UUID run);
    List<TypePersistence> findTypePersistenceByRunIdAndParameterType(UUID run , ParameterType parameterType);
}
