package be.cytomine.dto.appengine.task;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskRunValue {
    @JsonProperty("task_run_id")
    private UUID taskRunId;

    @JsonProperty("param_name")
    private String parameterName;

    private String type;

    private Object value;
}
