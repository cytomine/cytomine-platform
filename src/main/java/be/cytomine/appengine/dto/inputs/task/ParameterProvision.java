package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.models.task.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ParameterProvision {
    private ParameterType type;
}
