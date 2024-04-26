package be.cytomine.appengine.models.task;

import be.cytomine.appengine.models.task.bool.BooleanType;
import be.cytomine.appengine.models.task.integer.IntegerType;
import com.fasterxml.jackson.databind.JsonNode;

import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerTypeConstraint;
import jakarta.validation.constraints.NotNull;

public class TypeFactory {

    private static String getTypeId(JsonNode typeNode) {
        if (typeNode.isTextual()) {
            return typeNode.textValue();
        } else {
            return typeNode.get("id").textValue();
        }
    }

    public static Type createType(JsonNode node) {
        JsonNode typeNode = node.get("type");
        // add new types here
        String typeId = getTypeId(typeNode);
        return switch (typeId) {
            case "boolean" -> createBooleanType(typeId);
            case "integer" -> createIntegerType(typeNode, typeId);
            default -> new Type();
        };
    }

    @NotNull
    private static BooleanType createBooleanType(String typeId) {
        BooleanType type = new BooleanType();
        type.setId(typeId);

        return type;
    }

    @NotNull
    private static IntegerType createIntegerType(JsonNode typeNode, String typeId) {
        IntegerType type = new IntegerType();
        type.setId(typeId);

        for (IntegerTypeConstraint constraint : IntegerTypeConstraint.values()) {
            String constraintStringKey = constraint.getStringKey();
            if (typeNode.has(constraintStringKey)) {
                type.setConstraint(constraint, typeNode.get(constraintStringKey).asInt());
            }
        }

        return type;
    }
}
