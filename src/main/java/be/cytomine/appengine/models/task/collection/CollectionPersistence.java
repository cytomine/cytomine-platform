package be.cytomine.appengine.models.task.collection;

import java.util.List;

import be.cytomine.appengine.models.task.TypePersistence;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "enumeration_type_persistence")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class CollectionPersistence extends TypePersistence {

    private Integer size;
    private List<TypePersistence> items;
}
