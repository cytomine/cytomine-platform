package be.cytomine.appengine.models.filestorage;

import lombok.Data;

@Data
public class FileReference {

    private String fileId;
    public FileReference(String idFile) {
    }
}
