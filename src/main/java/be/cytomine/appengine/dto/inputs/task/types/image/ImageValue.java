package be.cytomine.appengine.dto.inputs.task.types.image;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageValue extends TaskRunParameterValue {
    private byte[] value;
}
