package be.cytomine.appengine.repositories.string;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.string.StringPersistence;

import java.util.UUID;

@Repository
public interface StringPersistenceRepository extends JpaRepository<StringPersistence, UUID> {
    StringPersistence findStringPersistenceByParameterNameAndRunIdAndParameterType(String parameterName, UUID run, ParameterType parameterType);
}
