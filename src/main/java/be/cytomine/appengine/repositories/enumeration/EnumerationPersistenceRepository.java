package be.cytomine.appengine.repositories.enumeration;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.enumeration.EnumerationPersistence;

@SuppressWarnings("checkstyle:LineLength")
@Repository
public interface EnumerationPersistenceRepository extends JpaRepository<EnumerationPersistence, UUID> {
    EnumerationPersistence findEnumerationPersistenceByParameterNameAndRunIdAndParameterType(String parameterName, UUID run, ParameterType parameterType);
}
