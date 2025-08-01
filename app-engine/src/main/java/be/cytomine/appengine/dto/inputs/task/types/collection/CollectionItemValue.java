package be.cytomine.appengine.dto.inputs.task.types.collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"task_run_id", "type", "param_name"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionItemValue extends TaskRunParameterValue {

    @JsonProperty("index")
    protected int index;
    private Object value;
}
