package be.cytomine.appengine.models.task;

public enum ParameterType {
    INPUT,
    OUTPUT;

    public static ParameterType from(String reference) {
        if ("inputs".equalsIgnoreCase(reference)) {
            return INPUT;
        } else if ("outputs".equalsIgnoreCase(reference)) {
            return OUTPUT;
        } else {
            throw new IllegalArgumentException("Invalid folder name: " + reference);
        }
    }
}
