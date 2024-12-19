package be.cytomine.appengine.repositories.file;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.file.FilePersistence;

@Repository
public interface FilePersistenceRepository extends JpaRepository<FilePersistence, UUID> {
    FilePersistence findFilePersistenceByParameterNameAndRunIdAndParameterType(String parameterName, UUID run, ParameterType parameterType);
}