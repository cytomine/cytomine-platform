package be.cytomine.appengine.models.task;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import be.cytomine.appengine.dto.inputs.task.types.enumeration.EnumerationTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.file.FileTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.image.ImageTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.number.NumberTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.string.StringTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.wsi.WsiTypeConstraint;
import be.cytomine.appengine.models.task.bool.BooleanType;
import be.cytomine.appengine.models.task.enumeration.EnumerationType;
import be.cytomine.appengine.models.task.file.FileType;
import be.cytomine.appengine.models.task.geometry.GeometryType;
import be.cytomine.appengine.models.task.image.ImageType;
import be.cytomine.appengine.models.task.integer.IntegerType;
import be.cytomine.appengine.models.task.number.NumberType;
import be.cytomine.appengine.models.task.string.StringType;
import be.cytomine.appengine.models.task.wsi.WsiType;


public class TypeFactory {

    public static String getTypeId(JsonNode typeNode) {
        if (typeNode.isTextual()) {
            return typeNode.textValue();
        } else {
            return typeNode.get("id").textValue();
        }
    }

    public static Type createType(JsonNode node , String charset) {
        JsonNode typeNode = node.get("type");
        // add new types here
        String typeId = getTypeId(typeNode);
        return switch (typeId) {
            case "boolean" -> createBooleanType(typeId , charset);
            case "integer" -> createIntegerType(typeNode, typeId , charset);
            case "number" -> createNumberType(typeNode, typeId , charset);
            case "string" -> createStringType(typeNode, typeId , charset);
            case "enumeration" -> createEnumerationType(typeNode, typeId , charset);
            case "geometry" -> createGeometryType(typeId , charset);
            case "image" -> createImageType(typeNode, typeId , charset);
            case "wsi" -> createWsiType(typeNode, typeId , charset);
            case "file" -> createFileType(typeNode, typeId , charset);
            default -> new Type();
        };
    }

    @NotNull
    private static BooleanType createBooleanType(String typeId , String charset) {
        BooleanType type = new BooleanType();
        type.setId(typeId);
        type.setCharset(charset);

        return type;
    }

    @NotNull
    private static IntegerType createIntegerType(JsonNode typeNode, String typeId , String charset) {
        IntegerType type = new IntegerType();
        type.setId(typeId);
        type.setCharset(charset);

        Arrays.stream(IntegerTypeConstraint.values())
            .map(IntegerTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(IntegerTypeConstraint.getConstraint(key), typeNode.get(key).asInt()));

        return type;
    }

    @NotNull
    private static NumberType createNumberType(JsonNode typeNode, String typeId , String charset) {
        NumberType type = new NumberType();
        type.setId(typeId);
        type.setCharset(charset);

        Arrays.stream(NumberTypeConstraint.values())
            .map(NumberTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(NumberTypeConstraint.getConstraint(key), typeNode.get(key).asText()));

        return type;
    }

    @NotNull
    private static StringType createStringType(JsonNode typeNode, String typeId , String charset) {
        StringType type = new StringType();
        type.setId(typeId);
        type.setCharset(charset);

        Arrays.stream(StringTypeConstraint.values())
            .map(StringTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(StringTypeConstraint.getConstraint(key), typeNode.get(key).asInt()));

        return type;
    }

    @NotNull
    private static EnumerationType createEnumerationType(JsonNode typeNode, String typeId , String charset) {
        EnumerationType type = new EnumerationType();
        type.setId(typeId);
        type.setCharset(charset);

        Arrays.stream(EnumerationTypeConstraint.values())
            .map(EnumerationTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(EnumerationTypeConstraint.getConstraint(key), typeNode.get(key).toString()));

        return type;
    }

    @NotNull
    private static GeometryType createGeometryType(String typeId , String charset) {
        GeometryType type = new GeometryType();
        type.setId(typeId);
        type.setCharset(charset);

        return type;
    }

    @NotNull
    private static ImageType createImageType(JsonNode typeNode, String typeId , String charset) {
        ImageType type = new ImageType();
        type.setId(typeId);
        type.setCharset(charset);
        Arrays.stream(ImageTypeConstraint.values())
            .map(ImageTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(ImageTypeConstraint.getConstraint(key), typeNode.get(key)));

        return type;
    }

    @NotNull
    private static WsiType createWsiType(JsonNode typeNode, String typeId , String charset) {
        WsiType type = new WsiType();
        type.setId(typeId);
        type.setCharset(charset);
        Arrays.stream(WsiTypeConstraint.values())
            .map(WsiTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(WsiTypeConstraint.getConstraint(key), typeNode.get(key)));

        return type;
    }

    @NotNull
    private static FileType createFileType(JsonNode typeNode, String typeId , String charset) {
        FileType type = new FileType();
        type.setId(typeId);
        type.setCharset(charset);

        Arrays.stream(FileTypeConstraint.values())
            .map(FileTypeConstraint::getStringKey)
            .filter(typeNode::has)
            .forEach(key -> type.setConstraint(FileTypeConstraint.getConstraint(key), typeNode.get(key)));

        return type;
    }
}
