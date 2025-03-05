package be.cytomine.appengine.models.task;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.models.BaseEntity;

@Entity
@Table(name = "author")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Author extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(generator = "UUID")
    @JsonIgnore
    UUID id;

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
