package be.cytomine.appengine.repositories;

import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RunRepository extends JpaRepository<Run, UUID> {
}
