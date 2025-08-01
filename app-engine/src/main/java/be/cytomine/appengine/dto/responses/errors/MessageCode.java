package be.cytomine.appengine.dto.responses.errors;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class MessageCode {
    public String code;
    public String message;
}
