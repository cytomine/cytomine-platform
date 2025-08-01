package be.cytomine.appengine.dto.inputs.task.types.enumeration;

import java.util.Arrays;

public enum EnumerationTypeConstraint {
    VALUES;

    public String getStringKey() {
        return switch (this) {
            case VALUES -> "values";
            default -> throw new RuntimeException("Unknown constraint");
        };
    }

    public static EnumerationTypeConstraint getConstraint(String key) {
        String error = "Invalid enumeration type constraint key: " + key;
        return Arrays.stream(EnumerationTypeConstraint.values())
            .filter(constraint -> constraint.getStringKey().equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(error));
    }
}
