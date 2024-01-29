package be.cytomine.appengine.dto.inputs.task;



import be.cytomine.appengine.models.task.Task;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/*
* {
  "name": "string",
  "namespace": "string",
  "version": "string",
  "description": "string",
  "authors": [
    {
      "first_name": "string",
      "last_name": "string",
      "organization": "string",
      "email": "string",
      "is_contact": true
    }
  ]
}*/
@Data
@AllArgsConstructor
public class TaskDescription {
    @JsonProperty(defaultValue = "")
    private String name;
    @JsonProperty(defaultValue = "")
    private String namespace;
    @JsonProperty(defaultValue = "")
    private String version;
    @JsonProperty(defaultValue = "")
    private String description;
    private Set<TaskAuthor> authors;

    public TaskDescription(String name, String namespace, String version, String description) {
        this.name = name;
        this.namespace = namespace;
        this.version = version;
        this.description = description == null? "" : description; // just to not return null in the response
    }
}
