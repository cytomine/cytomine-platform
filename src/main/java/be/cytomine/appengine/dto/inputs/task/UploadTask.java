package be.cytomine.appengine.dto.inputs.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UploadTask {

    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> errors;
    public UploadTask() {

    }
    public UploadTask(String message , List<String> errors) {
        this.message = message;
        this.errors = errors;
    }
}
