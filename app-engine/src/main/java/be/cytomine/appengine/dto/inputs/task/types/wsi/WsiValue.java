package be.cytomine.appengine.dto.inputs.task.types.wsi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class WsiValue extends TaskRunParameterValue {
    private byte[] value;
}
