package be.cytomine.appengine.dto.inputs.task.types.wsi;

import java.util.Arrays;

public enum WsiTypeConstraint {
    FORMATS,
    MAX_FILE_SIZE,
    MAX_WIDTH,
    MAX_HEIGHT;

    public String getStringKey() {
        return switch (this) {
            case FORMATS -> "formats";
            case MAX_FILE_SIZE -> "max_file_size";
            case MAX_WIDTH -> "max_width";
            case MAX_HEIGHT -> "max_height";
            default -> throw new RuntimeException("Unknown constraint");
        };
    }

    public static WsiTypeConstraint getConstraint(String key) {
        String error = "Invalid wsi type constraint key: " + key;
        return Arrays.stream(WsiTypeConstraint.values())
            .filter(constraint -> constraint.getStringKey().equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(error));
    }
}
