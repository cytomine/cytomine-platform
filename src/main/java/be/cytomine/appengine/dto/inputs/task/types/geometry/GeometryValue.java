package be.cytomine.appengine.dto.inputs.task.types.geometry;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeometryValue extends TaskRunParameterValue {
    private String value;
}
