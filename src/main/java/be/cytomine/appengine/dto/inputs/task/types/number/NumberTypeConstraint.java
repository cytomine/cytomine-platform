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
        switch (this) {
            case GREATER_EQUAL:
                return "geq";
            case LOWER_EQUAL:
                return "leq";
            case GREATER_THAN:
                return "gt";
            case LOWER_THAN:
                return "lt";
            case INFINITY_ALLOWED:
                return "infinity_allowed";
            case NAN_ALLOWED:
                return "nan_allowed";
            default:
                throw new RuntimeException("Unknown constraint");
        }
    }

    public static NumberTypeConstraint getConstraint(String key) {
        return Arrays.stream(NumberTypeConstraint.values())
                .filter(constraint -> constraint.getStringKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid number type constraint key: " + key));
    }
}
