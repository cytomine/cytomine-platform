package be.cytomine.appengine.models.task;

import java.util.UUID;

import be.cytomine.appengine.models.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "checksum")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Checksum extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private String reference; // identifier + file name
    private long checksumCRC32;

    public Checksum(UUID uuid, String reference,  long checksumCRC32)
    {
        super();
        this.id = uuid;
        this.reference = reference;
        this.checksumCRC32 = checksumCRC32;
    }
}
