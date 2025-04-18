package be.cytomine.appengine.dto.inputs.task.types.datetime;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import be.cytomine.appengine.dto.inputs.task.TaskParameterType;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TaskParameterDateTimeType extends TaskParameterType {
    private String id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant before;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant after;
}
