package be.cytomine.appengine.dto.inputs.task.types.integer;

import com.fasterxml.jackson.annotation.JsonInclude;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskParameterIntegerType extends TaskParameterType {


    private String id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer gt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer lt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer geq;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer leq;
}
