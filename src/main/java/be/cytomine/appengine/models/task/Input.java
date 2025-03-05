package be.cytomine.appengine.models.task;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.models.BaseEntity;

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
