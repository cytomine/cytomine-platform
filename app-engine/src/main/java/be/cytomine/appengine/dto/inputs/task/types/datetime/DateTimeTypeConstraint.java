package be.cytomine.appengine.dto.inputs.task.types.datetime;

import java.util.Arrays;

public enum DateTimeTypeConstraint {
    BEFORE,
    AFTER;

    public String getStringKey() {
        return switch (this) {
            case BEFORE -> "before";
            case AFTER -> "after";
            default -> throw new RuntimeException("Unknown constraint");
        };
    }

    public static DateTimeTypeConstraint getConstraint(String key) {
        String error = "Invalid number type constraint key: " + key;
        return Arrays.stream(DateTimeTypeConstraint.values())
            .filter(constraint -> constraint.getStringKey().equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(error));
    }
}
