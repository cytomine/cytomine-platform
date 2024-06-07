package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.dto.inputs.task.types.bool.TaskParameterBooleanType;
import be.cytomine.appengine.dto.inputs.task.types.integer.TaskParameterIntegerType;
import be.cytomine.appengine.models.task.Input;
import be.cytomine.appengine.models.task.bool.BooleanType;
import be.cytomine.appengine.models.task.integer.IntegerType;

public class TaskInputFactory {

    public static TaskInput createTaskInput(Input input) {
        TaskParameterType taskParameterType = null;

        if (input.getType() instanceof BooleanType type) {
            taskParameterType = new TaskParameterBooleanType(type.getId());
        } else if (input.getType() instanceof IntegerType type) {
            taskParameterType = new TaskParameterIntegerType(type.getId(), type.getGt(), type.getLt(), type.getGeq(), type.getLeq());
        }

        return new TaskInput(input.getId().toString(), input.getDefaultValue(), input.getName(), input.getDisplayName(), input.getDescription(), input.isOptional(), taskParameterType);
    }
}
