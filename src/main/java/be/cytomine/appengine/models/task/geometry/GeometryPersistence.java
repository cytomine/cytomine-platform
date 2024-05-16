package be.cytomine.appengine.models.task.geometry;

import be.cytomine.appengine.models.task.TypePersistence;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "geometry_type_persistence")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class GeometryPersistence extends TypePersistence {
    private String value;
}
