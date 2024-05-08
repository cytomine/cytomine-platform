package be.cytomine.appengine.dto.inputs.task.types.number;

import com.fasterxml.jackson.annotation.JsonInclude;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskParameterNumberType extends TaskParameterType {

    private String id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double gt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double geq;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double lt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double leq;

    private boolean infinityAllowed;
    private boolean nanAllowed;
}
