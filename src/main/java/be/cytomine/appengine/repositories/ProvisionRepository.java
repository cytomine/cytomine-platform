package be.cytomine.appengine.repositories;



import be.cytomine.appengine.models.task.Provision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProvisionRepository extends JpaRepository<Provision, UUID> {
    Provision findProvisionByParameterNameAndRunId(String parameterName, UUID run);
    List<Provision> findProvisionByRunId(UUID run);
}
