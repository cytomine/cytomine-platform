package be.cytomine.appengine.dto.inputs.task.types.number;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NumberValue extends TaskRunParameterValue {
    private double value;
}
