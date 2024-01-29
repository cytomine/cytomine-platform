package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.models.task.IntegerType;
import be.cytomine.appengine.models.task.PrimitiveProvision;
import be.cytomine.appengine.models.task.Type;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegerParameterProvision implements ParameterProvision{

    @JsonProperty(value = "param_name")
    private String parameterName;
    private int value;
    @JsonIgnore
    private String runId;

}
