package be.cytomine.appengine.dto.inputs.task.types.collection;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionValue extends TaskRunParameterValue {
    private List<TaskRunParameterValue> value;
}
