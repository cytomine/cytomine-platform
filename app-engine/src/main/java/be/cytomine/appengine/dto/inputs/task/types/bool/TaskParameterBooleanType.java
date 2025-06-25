package be.cytomine.appengine.dto.inputs.task.types.bool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskParameterBooleanType extends TaskParameterType {
    private String id;
}
