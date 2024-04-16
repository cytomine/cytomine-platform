package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.models.task.integer.IntegerType;
import be.cytomine.appengine.models.task.Output;

public class TaskOutputFactory {

    public static TaskOutput createTaskOutput(Output output) {
        if (output.getType() instanceof IntegerType type)
        {
            TaskParameterType taskParameterType = new TaskParameterIntegerType(type.getId(), type.getGt(), type.getLt(), type.getGeq(), type.getLeq());
            TaskOutput taskoutput = new TaskOutput(output.getId().toString(), output.getDefaultValue(), output.getName(), output.getDisplayName(), output.getDescription(), output.isOptional(), taskParameterType);
            return taskoutput;
        }
        return null;
    }
}
