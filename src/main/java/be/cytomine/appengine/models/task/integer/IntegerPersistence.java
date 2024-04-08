package be.cytomine.appengine.models.task.integer;

import be.cytomine.appengine.models.task.TypePersistence;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "integer_type_persistence")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class IntegerPersistence extends TypePersistence {
    private int value;
}
