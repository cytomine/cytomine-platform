package be.cytomine.appengine.models.task;

import be.cytomine.appengine.models.BaseEntity;
import jakarta.persistence.*;


import java.util.UUID;
@MappedSuperclass
public class PrimitiveProvision extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(generator = "UUID")
    protected UUID id;
}
