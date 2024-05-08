package be.cytomine.appengine.dto.inputs.task.types.bool;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BooleanValue extends TaskRunParameterValue {
    private boolean value;
}
