package be.cytomine.appengine.repositories;

import be.cytomine.appengine.models.task.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Task findByNamespaceAndVersion(String namespace, String version);
}
