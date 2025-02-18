package be.cytomine.appengine.dto.inputs.task.types.collection;

import java.util.Arrays;

public enum CollectionGenericTypeDependency
{
  DERIVED_FROM,
  MATCHING,
  ;

  public String getStringKey() {
    return switch (this) {
      case MATCHING -> "matching";
      case DERIVED_FROM -> "derived_from";
      default -> throw new RuntimeException("Unknown dependency");
    };
  }

  public static CollectionGenericTypeDependency getDependency(String key) {
    String error = "Invalid collection type generic dependency key: " + key;
    return Arrays.stream(CollectionGenericTypeDependency.values())
        .filter(constraint -> constraint.getStringKey().equals(key))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(error));
  }
}
