package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.dto.inputs.task.types.bool.TaskParameterBooleanType;
import be.cytomine.appengine.dto.inputs.task.types.integer.TaskParameterIntegerType;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.bool.BooleanType;
import be.cytomine.appengine.models.task.integer.IntegerType;

public class TaskOutputFactory {

    public static TaskOutput createTaskOutput(Output output) {
        TaskParameterType taskParameterType = null;

        if (output.getType() instanceof BooleanType type) {
            taskParameterType = new TaskParameterBooleanType(type.getId());
        } else if (output.getType() instanceof IntegerType type) {
            taskParameterType = new TaskParameterIntegerType(type.getId(), type.getGt(), type.getLt(), type.getGeq(), type.getLeq());
        }

        return new TaskOutput(output.getId().toString(), output.getDefaultValue(), output.getName(), output.getDisplayName(), output.getDescription(), output.isOptional(), taskParameterType);
    }
}
