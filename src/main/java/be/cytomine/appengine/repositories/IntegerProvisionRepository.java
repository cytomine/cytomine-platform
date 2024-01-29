package be.cytomine.appengine.repositories;

import be.cytomine.appengine.models.task.IntegerProvision;
import be.cytomine.appengine.models.task.Run;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IntegerProvisionRepository extends JpaRepository<IntegerProvision, UUID> {
    IntegerProvision findIntegerProvisionByParameterNameAndRunId(String parameterName, UUID run);
    List<IntegerProvision> findIntegerProvisionByRunId(UUID run);
}
