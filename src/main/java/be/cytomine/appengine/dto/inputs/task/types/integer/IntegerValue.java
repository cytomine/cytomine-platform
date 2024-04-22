package be.cytomine.appengine.dto.inputs.task.types.integer;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegerValue extends TaskRunParameterValue {
    private int value;
}
