package be.cytomine.appengine.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import be.cytomine.appengine.models.task.Run;

@Repository
public interface RunRepository extends JpaRepository<Run, UUID> {}
