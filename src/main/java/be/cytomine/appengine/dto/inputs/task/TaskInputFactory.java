package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.models.task.Input;
import be.cytomine.appengine.models.task.IntegerType;

public class TaskInputFactory {

    public static TaskInput createTaskInput(Input input) {
        if (input.getType() instanceof IntegerType type)
        {
            TaskParameterType taskParameterType = new TaskParameterIntegerType(type.getId(), type.getGt(), type.getLt(), type.getGeq(), type.getLeq());
            TaskInput taskInput = new TaskInput(input.getId().toString(), input.getDefaultValue(), input.getName(), input.getDisplayName(), input.getDescription(), input.isOptional(), taskParameterType);
            return taskInput;
        }
        return null;
    }
}
