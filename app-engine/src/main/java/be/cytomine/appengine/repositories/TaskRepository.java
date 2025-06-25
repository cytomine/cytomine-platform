package be.cytomine.appengine.repositories;

import java.util.UUID;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Task findByNamespaceAndVersion(String namespace, String version);

    @Modifying
    @Transactional
    @Query("DELETE FROM Task")
    void deleteAllTasks();
}
