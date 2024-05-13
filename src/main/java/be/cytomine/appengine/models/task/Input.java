package be.cytomine.appengine.models.task;

import be.cytomine.appengine.models.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Entity
@Table(name = "input")
@Data
@EqualsAndHashCode(callSuper = true)
public class Input extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(generator = "UUID")
    private UUID id;
    private String defaultValue; // this matches a reserved keyword
    private String name;
    private String displayName;
    private String description;
    private boolean optional;

    @OneToOne(cascade = CascadeType.ALL)
    private Type type;
}
