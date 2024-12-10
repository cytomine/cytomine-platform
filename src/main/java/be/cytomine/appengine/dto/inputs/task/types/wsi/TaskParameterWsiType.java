package be.cytomine.appengine.dto.inputs.task.types.wsi;

import java.util.List;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class TaskParameterWsiType extends TaskParameterType {
    private String id;

    private List<String> formats;
}
