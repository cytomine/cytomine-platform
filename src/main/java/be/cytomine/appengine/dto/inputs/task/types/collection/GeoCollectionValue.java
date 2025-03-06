package be.cytomine.appengine.dto.inputs.task.types.collection;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoCollectionValue extends CollectionValue {
    // use field hiding to hide value from parent
    private String value;
}
