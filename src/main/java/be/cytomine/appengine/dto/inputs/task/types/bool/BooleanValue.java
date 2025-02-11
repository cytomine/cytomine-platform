package be.cytomine.appengine.dto.inputs.task.types.bool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BooleanValue extends TaskRunParameterValue {
    private boolean value;
}
