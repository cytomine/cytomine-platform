package be.cytomine.appengine.dto.inputs.task.types.enumeration;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskParameterEnumerationType extends TaskParameterType {
    private String id;

    private List<String> values;
}
