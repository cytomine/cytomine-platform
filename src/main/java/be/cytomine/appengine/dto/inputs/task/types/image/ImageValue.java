package be.cytomine.appengine.dto.inputs.task.types.image;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageValue extends TaskRunParameterValue {
    private byte[] value;
}
