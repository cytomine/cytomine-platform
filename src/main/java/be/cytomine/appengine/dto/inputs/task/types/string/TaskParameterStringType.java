package be.cytomine.appengine.dto.inputs.task.types.string;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskParameterStringType extends TaskParameterType {
    private String id;

    private Integer minLength;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer maxLength;
}
