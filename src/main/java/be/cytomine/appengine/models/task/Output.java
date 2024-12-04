package be.cytomine.appengine.models.task;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.models.BaseEntity;

@Entity
@Table(name = "output")
@Data
@EqualsAndHashCode(callSuper = true)
public class Output extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(generator = "UUID")
    private UUID id;
    private int defaultValue; // this matches a reserved keyword
    private String name;
    private String displayName;
    private String description;
    private boolean optional;
    @OneToOne(cascade = CascadeType.ALL)
    private Type type;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "derived_from", nullable = true)
    private Input derivedFrom;
}
