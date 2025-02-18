package be.cytomine.appengine.dto.inputs.task.types.collection;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionItemValue extends TaskRunParameterValue {
    private TaskRunParameterValue value;
}
