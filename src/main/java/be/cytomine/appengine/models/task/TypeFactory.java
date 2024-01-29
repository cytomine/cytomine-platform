package be.cytomine.appengine.models.task;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class TypeFactory {

    private static String getTypeId(JsonNode typeNode) {
        if (typeNode.isTextual()) {
            return typeNode.textValue();
        } else  {
            return typeNode.get("id").textValue();
        }
    }

    public static Type createType(JsonNode node) {
        JsonNode typeNode = node.get("type");
        // add new types here
        String typeId = getTypeId(typeNode);
        if (typeId.equals("integer")) {
            return createIntegerType(typeNode, typeId);
        } else {
            return new Type();
        }
    }

    @NotNull
    private static IntegerType createIntegerType(JsonNode typeNode, String typeId) {
        IntegerType type = new IntegerType();
        type.setConstraints(new ArrayList<String>());
        if (typeNode.get("lt") != null){
            type.setLt(typeNode.get("lt").asInt());
            type.getConstraints().add("lt");
        }
        if (typeNode.get("gt") != null){
            type.setGt(typeNode.get("gt").asInt());
            type.getConstraints().add("gt");}
        if (typeNode.get("let") != null){
            type.setLt(typeNode.get("leq").asInt());
            type.getConstraints().add("leq");
        }
        if (typeNode.get("get") != null){
            type.setLt(typeNode.get("geq").asInt());
            type.getConstraints().add("geq");}
        type.setId(typeId);
        return type;
    }
}
