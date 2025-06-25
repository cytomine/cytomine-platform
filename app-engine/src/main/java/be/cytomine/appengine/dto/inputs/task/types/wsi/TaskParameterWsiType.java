package be.cytomine.appengine.dto.inputs.task.types.wsi;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class TaskParameterWsiType extends TaskParameterType {
    private String id;

    private List<String> formats;
}
