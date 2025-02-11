package be.cytomine.appengine.dto.inputs.task.types.integer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegerValue extends TaskRunParameterValue {
    private int value;
}
