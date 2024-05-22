package be.cytomine.appengine.dto.inputs.task.types.enumeration;

import java.util.Arrays;

public enum EnumerationTypeConstraint {
    VALUES;

    public String getStringKey() {
        switch (this) {
            case VALUES:
                return "values";
            default:
                throw new RuntimeException("Unknown constraint");
        }
    }

    public static EnumerationTypeConstraint getConstraint(String key) {
        return Arrays.stream(EnumerationTypeConstraint.values())
                .filter(constraint -> constraint.getStringKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid enumeration type constraint key: " + key));
    }
}
