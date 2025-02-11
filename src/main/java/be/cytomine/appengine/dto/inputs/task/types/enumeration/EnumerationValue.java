package be.cytomine.appengine.dto.inputs.task.types.enumeration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnumerationValue extends TaskRunParameterValue {
    private String value;
}
