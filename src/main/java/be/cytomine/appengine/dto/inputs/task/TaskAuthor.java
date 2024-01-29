package be.cytomine.appengine.dto.inputs.task;

import be.cytomine.appengine.models.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;



@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class TaskAuthor {

    @JsonProperty("first_name")
    String firstName;
    @JsonProperty("last_name")
    String lastName;
    @JsonProperty("organization")
    String organization;
    @JsonProperty("email")
    String email;
    @JsonProperty("is_contact")
    boolean isContact;

}
