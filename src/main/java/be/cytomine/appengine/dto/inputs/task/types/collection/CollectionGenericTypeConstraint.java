package be.cytomine.appengine.dto.inputs.task.types.collection;

import java.util.Arrays;

public enum CollectionGenericTypeConstraint {
  MAX_SIZE,
  MIN_SIZE,
  ;

  public String getStringKey() {
    return switch (this) {
      case MAX_SIZE -> "max_size";
      case MIN_SIZE -> "min_size";
      default -> throw new RuntimeException("Unknown constraint");
    };
  }

  public static CollectionGenericTypeConstraint getConstraint(String key) {
    String error = "Invalid collection type generic constraint key: " + key;
    return Arrays.stream(CollectionGenericTypeConstraint.values())
        .filter(constraint -> constraint.getStringKey().equals(key))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(error));
  }
}
