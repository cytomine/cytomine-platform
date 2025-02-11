package be.cytomine.appengine.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Task findByNamespaceAndVersion(String namespace, String version);
}
