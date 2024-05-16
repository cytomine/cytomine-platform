package be.cytomine.appengine.dto.inputs.task.types.geometry;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskparameterGeometryType extends TaskParameterType {
    private String id;
}
