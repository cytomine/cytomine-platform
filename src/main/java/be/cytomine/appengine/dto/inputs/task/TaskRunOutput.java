package be.cytomine.appengine.dto.inputs.task;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class TaskRunOutput {
    private String type;
    private int value;
    private UUID task_run_id;
    private String param_name;
}
