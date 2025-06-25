package be.cytomine.appengine.dto.inputs.task;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.models.task.ValueType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRunParameterValue {
    @JsonProperty("task_run_id")
    protected UUID taskRunId;

    @JsonProperty("param_name")
    protected String parameterName;

    protected ValueType type;
}
