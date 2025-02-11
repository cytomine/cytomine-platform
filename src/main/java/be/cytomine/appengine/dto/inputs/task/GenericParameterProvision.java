package be.cytomine.appengine.dto.inputs.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericParameterProvision extends ParameterProvision {
    @JsonProperty(value = "param_name")
    private String parameterName;

    private Object value; // this should be Object now to accommodate other primitive types

    @JsonIgnore
    private String runId;
}
