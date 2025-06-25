package be.cytomine.appengine.dto.inputs.task.types.geometry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskParameterGeometryType extends TaskParameterType {
    private String id;
}
