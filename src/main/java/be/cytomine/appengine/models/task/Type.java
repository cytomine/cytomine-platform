package be.cytomine.appengine.models.task;

import be.cytomine.appengine.models.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "type")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(callSuper = true)

public class Type extends BaseEntity{
    @Id
    @Column(name = "identifier", updatable = false, nullable = false)
    @GeneratedValue(generator = "UUID")
    private UUID identifier;

    @ElementCollection
    private List<String> constraints; // used to track which constraints are defined for this type object
}
