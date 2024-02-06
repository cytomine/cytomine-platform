package be.cytomine.appengine.dto.inputs.task.types.integer;

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
    switch (this) {
      case GREATER_EQUAL:
        return "geq";
      case LOWER_EQUAL:
        return "leq";
      case GREATER_THAN:
        return "gt";
      case LOWER_THAN:
        return "lt";
      default:
        throw new RuntimeException("Unknown constraint");
    }
  }

  /**
   * Return the constraint for the given key.
   * @param key The constraint key
   * @return constraint
   */
  public static IntegerTypeConstraint getConstraint(String key) {
    for (IntegerTypeConstraint constraint : IntegerTypeConstraint.values()) {
      if (constraint.getStringKey().equals(key)) {
        return constraint;
      }
    }
    throw new IllegalArgumentException("Invalid integer type constraint key: " + key);
  }
}
