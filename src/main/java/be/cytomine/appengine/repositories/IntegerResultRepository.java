package be.cytomine.appengine.repositories;

import be.cytomine.appengine.models.task.IntegerProvision;
import be.cytomine.appengine.models.task.IntegerResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntegerResultRepository extends JpaRepository<IntegerResult, UUID> {
    IntegerResult findIntegerResultByParameterNameAndRunId(String parameterName , UUID run);
    List<IntegerResult> findIntegerResultByRunId(UUID run);
}
