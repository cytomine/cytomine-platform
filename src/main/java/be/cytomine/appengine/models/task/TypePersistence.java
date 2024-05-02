package be.cytomine.appengine.models.task;

import be.cytomine.appengine.models.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.UUID;
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypePersistence extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(generator = "UUID")
    protected UUID id;
    protected String parameterName;
    protected UUID runId;
    protected ValueType valueType;
    protected ParameterType parameterType;
}
