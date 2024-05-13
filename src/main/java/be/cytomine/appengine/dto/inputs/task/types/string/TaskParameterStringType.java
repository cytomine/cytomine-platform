package be.cytomine.appengine.dto.inputs.task.types.string;

import com.fasterxml.jackson.annotation.JsonInclude;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskParameterStringType extends TaskParameterType {

    private String id;

    private Integer minLength;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer maxLength;
}
