package be.cytomine.appengine.repositories;


import be.cytomine.appengine.models.task.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResultRepository extends JpaRepository<Result, UUID> {
    Result findResultByParameterNameAndRunId(String parameterName , UUID run);
    List<Result> findResultByRunId(UUID run);
}
