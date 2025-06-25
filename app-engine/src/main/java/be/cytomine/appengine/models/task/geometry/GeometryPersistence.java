package be.cytomine.appengine.models.task.geometry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import be.cytomine.appengine.models.task.TypePersistence;

@Entity
@Table(name = "geometry_type_persistence")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class GeometryPersistence extends TypePersistence {
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String value;
}
