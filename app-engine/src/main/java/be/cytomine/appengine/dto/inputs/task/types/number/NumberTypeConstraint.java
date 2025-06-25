package be.cytomine.appengine.dto.inputs.task.types.number;

import java.util.Arrays;

public enum NumberTypeConstraint {
    GREATER_THAN,
    LOWER_THAN,
    GREATER_EQUAL,
    LOWER_EQUAL,
    INFINITY_ALLOWED,
    NAN_ALLOWED;

    public String getStringKey() {
        return switch (this) {
            case GREATER_EQUAL -> "geq";
            case LOWER_EQUAL -> "leq";
            case GREATER_THAN -> "gt";
            case LOWER_THAN -> "lt";
            case INFINITY_ALLOWED -> "infinity_allowed";
            case NAN_ALLOWED -> "nan_allowed";
            default -> throw new RuntimeException("Unknown constraint");
        };
    }

    public static NumberTypeConstraint getConstraint(String key) {
        String error = "Invalid number type constraint key: " + key;
        return Arrays.stream(NumberTypeConstraint.values())
            .filter(constraint -> constraint.getStringKey().equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(error));
    }
}
