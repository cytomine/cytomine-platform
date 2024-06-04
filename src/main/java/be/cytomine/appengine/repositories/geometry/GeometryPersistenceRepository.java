package be.cytomine.appengine.repositories.geometry;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.geometry.GeometryPersistence;

@Repository
public interface GeometryPersistenceRepository extends JpaRepository<GeometryPersistence, UUID> {
    GeometryPersistence findGeometryPersistenceByParameterNameAndRunIdAndParameterType(String parameterName, UUID run, ParameterType parameterType);
}
