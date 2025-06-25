package be.cytomine.appengine.dto.inputs.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GenericParameterCollectionItemProvision extends ParameterProvision {
    @JsonProperty(value = "index")
    private String index;
    private Object value; // this should be Object now to accommodate other primitive types
    @JsonIgnore
    private String parameterName;
    @JsonIgnore
    private String runId;
}
