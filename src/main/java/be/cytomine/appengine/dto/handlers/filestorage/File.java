package be.cytomine.appengine.dto.handlers.filestorage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class File {
    String path; // path where?
    String idFile;

    public File(String idFile) {
        this.idFile = idFile;
    }
}
