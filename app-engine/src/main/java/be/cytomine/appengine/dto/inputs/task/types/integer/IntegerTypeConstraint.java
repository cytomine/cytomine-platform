package be.cytomine.appengine.dto.inputs.task.types.integer;

import java.util.Arrays;

public enum IntegerTypeConstraint {
    GREATER_THAN,
    LOWER_THAN,
    GREATER_EQUAL,
    LOWER_EQUAL;

    /**
     * Return the string identifier for the constraints.
     * This identifier is used as key in the
     */
    public String getStringKey() {
        return switch (this) {
            case GREATER_EQUAL -> "geq";
            case LOWER_EQUAL -> "leq";
            case GREATER_THAN -> "gt";
            case LOWER_THAN -> "lt";
            default -> throw new RuntimeException("Unknown constraint");
        };
    }

    /**
     * Return the constraint for the given key.
     *
     * @param key The constraint key
     * @return constraint
     */
    public static IntegerTypeConstraint getConstraint(String key) {
        String error = "Invalid integer type constraint key: " + key;
        return Arrays.stream(IntegerTypeConstraint.values())
            .filter(constraint -> constraint.getStringKey().equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(error));
    }
}
