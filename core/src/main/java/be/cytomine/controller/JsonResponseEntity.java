package be.cytomine.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class JsonResponseEntity {
    public static ResponseEntity.BodyBuilder status(HttpStatus status) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON);
    }

    public static ResponseEntity.BodyBuilder status(HttpStatusCode status) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON);
    }
}
