package be.cytomine.appengine.repositories.wsi;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.wsi.WsiPersistence;

@Repository
public interface WsiPersistenceRepository extends JpaRepository<WsiPersistence, UUID> {
    WsiPersistence findWsiPersistenceByParameterNameAndRunIdAndParameterType(String parameterName, UUID run, ParameterType parameterType);
}
