package be.cytomine.appengine.repositories;

import java.util.UUID;

import be.cytomine.appengine.models.task.Checksum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecksumRepository extends JpaRepository<Checksum, UUID> {
    Checksum findByReference(String reference);
}
