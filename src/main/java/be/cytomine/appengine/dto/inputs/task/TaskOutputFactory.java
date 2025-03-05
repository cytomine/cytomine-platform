package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.dto.inputs.task.types.bool.TaskParameterBooleanType;
import be.cytomine.appengine.dto.inputs.task.types.enumeration.TaskParameterEnumerationType;
import be.cytomine.appengine.dto.inputs.task.types.file.TaskParameterFileType;
import be.cytomine.appengine.dto.inputs.task.types.geometry.TaskParameterGeometryType;
import be.cytomine.appengine.dto.inputs.task.types.image.TaskParameterImageType;
import be.cytomine.appengine.dto.inputs.task.types.integer.TaskParameterIntegerType;
import be.cytomine.appengine.dto.inputs.task.types.number.TaskParameterNumberType;
import be.cytomine.appengine.dto.inputs.task.types.string.TaskParameterStringType;
import be.cytomine.appengine.dto.inputs.task.types.wsi.TaskParameterWsiType;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.bool.BooleanType;
import be.cytomine.appengine.models.task.enumeration.EnumerationType;
import be.cytomine.appengine.models.task.file.FileType;
import be.cytomine.appengine.models.task.geometry.GeometryType;
import be.cytomine.appengine.models.task.image.ImageType;
import be.cytomine.appengine.models.task.integer.IntegerType;
import be.cytomine.appengine.models.task.number.NumberType;
import be.cytomine.appengine.models.task.string.StringType;
import be.cytomine.appengine.models.task.wsi.WsiType;

public class TaskOutputFactory {

    public static TaskOutput createTaskOutput(Output output) {
        TaskParameterType taskParameterType = null;

        if (output.getType() instanceof BooleanType type) {
            taskParameterType = new TaskParameterBooleanType(
                type.getId()
            );
        } else if (output.getType() instanceof IntegerType type) {
            taskParameterType = new TaskParameterIntegerType(
                type.getId(),
                type.getGt(),
                type.getLt(),
                type.getGeq(),
                type.getLeq()
            );
        } else if (output.getType() instanceof NumberType type) {
            taskParameterType = new TaskParameterNumberType(
                type.getId(),
                type.getGt(),
                type.getGeq(),
                type.getLt(),
                type.getLeq(),
                type.isInfinityAllowed(),
                type.isNanAllowed()
            );
        } else if (output.getType() instanceof StringType type) {
            taskParameterType = new TaskParameterStringType(
                type.getId(),
                type.getMinLength(),
                type.getMaxLength()
            );
        } else if (output.getType() instanceof EnumerationType type) {
            taskParameterType = new TaskParameterEnumerationType(
                type.getId(),
                type.getValues()
            );
        } else if (output.getType() instanceof GeometryType type) {
            taskParameterType = new TaskParameterGeometryType(
                type.getId()
            );
        } else if (output.getType() instanceof ImageType type) {
            taskParameterType = new TaskParameterImageType(
                type.getId(),
                type.getFormats()
            );
        } else if (output.getType() instanceof WsiType type) {
            taskParameterType = new TaskParameterWsiType(
                type.getId(),
                type.getFormats()
            );
        } else if (output.getType() instanceof FileType type) {
            taskParameterType = new TaskParameterFileType(
                type.getId(),
                type.getFormats()
            );
        }

        return new TaskOutput(
            output.getId().toString(),
            output.getDefaultValue(),
            output.getName(),
            output.getDisplayName(),
            output.getDescription(),
            output.isOptional(),
            taskParameterType,
            output.getDerivedFrom() != null ? output.getDerivedFrom().getId().toString() : null
        );
    }
}
