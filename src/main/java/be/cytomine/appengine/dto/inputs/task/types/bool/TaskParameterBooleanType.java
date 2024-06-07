package be.cytomine.appengine.dto.inputs.task.types.bool;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskParameterBooleanType extends TaskParameterType {
    private String id;
}
