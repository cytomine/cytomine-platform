package be.cytomine.appengine.models.task;

import be.cytomine.appengine.models.task.bool.BooleanType;
import be.cytomine.appengine.models.task.enumeration.EnumerationType;
import be.cytomine.appengine.models.task.geometry.GeometryType;
import be.cytomine.appengine.models.task.image.ImageType;
import be.cytomine.appengine.models.task.integer.IntegerType;
import be.cytomine.appengine.models.task.number.NumberType;
import be.cytomine.appengine.models.task.string.StringType;

import com.fasterxml.jackson.databind.JsonNode;

import be.cytomine.appengine.dto.inputs.task.types.enumeration.EnumerationTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.image.ImageTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.number.NumberTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.string.StringTypeConstraint;
import jakarta.validation.constraints.NotNull;

import java.util.Arrays;

public class TypeFactory {

    public static String getTypeId(JsonNode typeNode) {
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
            case "number" -> createNumberType(typeNode, typeId);
            case "string" -> createStringType(typeNode, typeId);
            case "enumeration" -> createEnumerationType(typeNode, typeId);
            case "geometry" -> createGeometryType(typeId);
            case "image" -> createImageType(typeNode, typeId);
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

        Arrays.stream(IntegerTypeConstraint.values())
            .map(IntegerTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(IntegerTypeConstraint.getConstraint(key), typeNode.get(key).asInt()));

        return type;
    }

    @NotNull
    private static NumberType createNumberType(JsonNode typeNode, String typeId) {
        NumberType type = new NumberType();
        type.setId(typeId);

        Arrays.stream(NumberTypeConstraint.values())
            .map(NumberTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(NumberTypeConstraint.getConstraint(key), typeNode.get(key).asText()));

        return type;
    }

    @NotNull
    private static StringType createStringType(JsonNode typeNode, String typeId) {
        StringType type = new StringType();
        type.setId(typeId);

        Arrays.stream(StringTypeConstraint.values())
            .map(StringTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(StringTypeConstraint.getConstraint(key), typeNode.get(key).asInt()));

        return type;
    }

    @NotNull
    private static EnumerationType createEnumerationType(JsonNode typeNode, String typeId) {
        EnumerationType type = new EnumerationType();
        type.setId(typeId);

        Arrays.stream(EnumerationTypeConstraint.values())
            .map(EnumerationTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(EnumerationTypeConstraint.getConstraint(key), typeNode.get(key).toString()));

        return type;
    }

    @NotNull
    private static GeometryType createGeometryType(String typeId) {
        GeometryType type = new GeometryType();
        type.setId(typeId);

        return type;
    }

    @NotNull
    private static ImageType createImageType(JsonNode typeNode, String typeId) {
        ImageType type = new ImageType();
        type.setId(typeId);

        Arrays.stream(ImageTypeConstraint.values())
            .map(ImageTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(ImageTypeConstraint.getConstraint(key), typeNode.get(key)));

        return type;
    }
}
