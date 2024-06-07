package be.cytomine.appengine.dto.inputs.task.types.number;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NumberValue extends TaskRunParameterValue {
    private double value;
}
