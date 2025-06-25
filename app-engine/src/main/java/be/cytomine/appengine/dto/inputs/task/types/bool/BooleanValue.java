package be.cytomine.appengine.dto.inputs.task.types.bool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BooleanValue extends TaskRunParameterValue {
    private boolean value;
}
