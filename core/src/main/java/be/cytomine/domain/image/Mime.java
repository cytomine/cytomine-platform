package be.cytomine.domain.image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;

@Entity
@Getter
@Setter
public class Mime extends CytomineDomain {

    @Size(min = 1, max = 5)
    private String extension;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String mimeType;

    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
