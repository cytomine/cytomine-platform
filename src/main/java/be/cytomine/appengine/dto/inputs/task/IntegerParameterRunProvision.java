package be.cytomine.appengine.dto.inputs.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegerParameterRunProvision{

    @JsonProperty(value = "param_name")
    private String parameterName;
    private int value;
    @JsonProperty(value = "task_run_id")
    private String runId;

}
