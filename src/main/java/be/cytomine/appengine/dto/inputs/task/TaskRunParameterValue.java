package be.cytomine.appengine.dto.inputs.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRunParameterValue {
    protected UUID task_run_id;
    protected String param_name;
}
