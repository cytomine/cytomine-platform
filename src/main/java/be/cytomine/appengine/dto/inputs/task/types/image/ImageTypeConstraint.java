package be.cytomine.appengine.dto.inputs.task.types.image;

import java.util.Arrays;

public enum ImageTypeConstraint {
    FORMATS,
    MAX_FILE_SIZE,
    MAX_WIDTH,
    MAX_HEIGHT;

    public String getStringKey() {
        switch (this) {
            case FORMATS:
                return "formats";
            case MAX_FILE_SIZE:
                return "max_file_size";
            case MAX_WIDTH:
                return "max_width";
            case MAX_HEIGHT:
                return "max_height";
            default:
                throw new RuntimeException("Unknown constraint");
        }
    }

    public static ImageTypeConstraint getConstraint(String key) {
        return Arrays.stream(ImageTypeConstraint.values())
                .filter(constraint -> constraint.getStringKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid image type constraint key: " + key));
    }
}
