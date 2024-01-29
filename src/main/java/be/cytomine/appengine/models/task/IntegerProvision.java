package be.cytomine.appengine.models.task;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "integer_provision")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class IntegerProvision extends PrimitiveProvision {
    private String parameterName;
    private int value;
    private UUID runId;

}
