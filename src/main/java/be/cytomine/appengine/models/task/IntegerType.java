package be.cytomine.appengine.models.task;

import be.cytomine.appengine.exceptions.TypeValidationException;
import be.cytomine.appengine.models.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class IntegerType extends Type  {

    final static Logger logger = LoggerFactory.getLogger(Type.class);

    private String id; // integer
    @Column(nullable = true)
    private Integer gt;
    @Column(nullable = true)
    private Integer lt;
    @Column(nullable = true)
    private Integer geq;
    @Column(nullable = true)
    private Integer leq;


}
