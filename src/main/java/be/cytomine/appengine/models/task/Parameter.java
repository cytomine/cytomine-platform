package be.cytomine.appengine.models.task;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.models.BaseEntity;

@Entity
@Table(name = "parameter")
@Data
@EqualsAndHashCode(callSuper = true)
public class Parameter extends BaseEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(generator = "UUID")
    private UUID id;
    private ParameterType parameterType;
    private String defaultValue; // this matches a reserved keyword
    private String name;
    private String displayName;
    private String description;
    private boolean optional;
    @OneToOne(cascade = CascadeType.ALL)
    private Type type;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "derived_from", nullable = true)
    private Parameter derivedFrom;
}
