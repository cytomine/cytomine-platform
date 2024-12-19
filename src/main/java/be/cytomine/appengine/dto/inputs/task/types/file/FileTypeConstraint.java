package be.cytomine.appengine.dto.inputs.task.types.file;

import java.util.Arrays;

public enum FileTypeConstraint {
    FORMATS,
    MAX_FILE_SIZE;

    public String getStringKey() {
        return switch (this) {
        case FORMATS -> "formats";
        case MAX_FILE_SIZE -> "max_file_size";
        default -> throw new RuntimeException("Unknown constraint");
        };
    }

    public static FileTypeConstraint getConstraint(String key) {
        return Arrays.stream(FileTypeConstraint.values())
                .filter(constraint -> constraint.getStringKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid file type constraint key: " + key));
    }
}
