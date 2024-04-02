package be.cytomine.appengine.models.task;

import be.cytomine.appengine.dto.inputs.task.types.integer.IntegerTypeConstraint;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.exceptions.TypeValidationException;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class IntegerType extends Type {

    final static Logger logger = LoggerFactory.getLogger(Type.class);


    @Column(nullable = true)
    private Integer gt;
    @Column(nullable = true)
    private Integer lt;
    @Column(nullable = true)
    private Integer geq;
    @Column(nullable = true)
    private Integer leq;

    public void setConstraint(IntegerTypeConstraint constraint, Integer value) {
        switch (constraint) {
            case GREATER_EQUAL:
                this.setGeq(value);
                break;
            case GREATER_THAN:
                this.setGt(value);
                break;
            case LOWER_EQUAL:
                this.setLeq(value);
                break;
            case LOWER_THAN:
                this.setLt(value);
                break;
        }
    }

    public boolean validate(Integer value) throws TypeValidationException {
        if (this.hasConstraint(IntegerTypeConstraint.GREATER_THAN) && value <= this.getGt())
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GT_VALIDATION_ERROR);
        if (this.hasConstraint(IntegerTypeConstraint.GREATER_EQUAL) && value < this.getGeq())
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_GEQ_VALIDATION_ERROR);
        if (this.hasConstraint(IntegerTypeConstraint.LOWER_THAN) && value >= this.getLt())
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_LT_VALIDATION_ERROR);
        if (this.hasConstraint(IntegerTypeConstraint.LOWER_EQUAL) && value > this.getLeq())
            throw new TypeValidationException(ErrorCode.INTERNAL_PARAMETER_LEQ_VALIDATION_ERROR);
        return true;
    }

    public boolean hasConstraint(IntegerTypeConstraint constraint) {
        switch (constraint) {
            case GREATER_EQUAL:
                return this.geq != null;
            case GREATER_THAN:
                return this.gt != null;
            case LOWER_EQUAL:
                return this.leq != null;
            case LOWER_THAN:
                return this.lt != null;
            default:
                return false;
        }
    }

    public boolean hasConstraint(String constraintKey) {
        return this.hasConstraint(IntegerTypeConstraint.getConstraint(constraintKey));
    }
}
