package be.cytomine.appengine.dto.inputs.task.types.string;

import java.util.Arrays;

public enum StringTypeConstraint {
    MIN_LENGTH,
    MAX_LENGTH;

    public String getStringKey() {
        return switch (this) {
            case MIN_LENGTH -> "min_length";
            case MAX_LENGTH -> "max_length";
            default -> throw new RuntimeException("Unknown constraint");
        };
    }

    public static StringTypeConstraint getConstraint(String key) {
        String error = "Invalid string type constraint key: " + key;
        return Arrays.stream(StringTypeConstraint.values())
            .filter(constraint -> constraint.getStringKey().equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(error));
    }
}
