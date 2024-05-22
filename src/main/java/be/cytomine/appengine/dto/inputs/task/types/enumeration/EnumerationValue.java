package be.cytomine.appengine.dto.inputs.task.types.enumeration;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnumerationValue extends TaskRunParameterValue {
    private String value;
}
