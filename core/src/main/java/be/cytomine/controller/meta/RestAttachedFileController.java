package be.cytomine.controller.meta;

import java.io.IOException;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.meta.AttachedFileService;
import be.cytomine.utils.JsonObject;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class RestAttachedFileController extends RestCytomineController {

    private final AttachedFileService attachedFileService;

    @GetMapping("/attachedfile.json")
    public ResponseEntity<String> list() {
        log.debug("REST request to list attached file");
        return responseSuccess(attachedFileService.list());
    }

    @GetMapping("/domain/{domainClassName}/{domainIdent}/attachedfile.json")
    public ResponseEntity<String> listByDomain(
            @PathVariable String domainClassName,
            @PathVariable Long domainIdent
    )  {
        log.debug("REST request to list attached file for domain {} {}", domainClassName, domainIdent);

        return responseSuccess(attachedFileService.findAllByDomain(domainClassName, domainIdent));
    }

    @GetMapping("/attachedfile/{id}.json")
    public ResponseEntity<String> show(@PathVariable Long id) {
        log.debug("REST request to show attached file {}", id);
        return responseSuccess(attachedFileService.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("AttachedFile", id)));
    }

    @GetMapping(value = {"/attachedfile/{id}/download.json", "/attachedfile/{id}/download"})
    public void download(@PathVariable Long id) throws IOException {
        log.debug("REST request to download attached file {}", id);
        AttachedFile attachedFile = attachedFileService.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("AttachedFile", id));
        responseFile(attachedFile.getFilename(), attachedFile.getData());
    }

    @RequestMapping(value = "/attachedfile.json", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<String> upload(
        @RequestParam(required = false)  Long domainIdent,
        @RequestParam(required = false)  String domainClassName,
        @RequestParam(required = false)  String filename,
        @RequestParam(required = false)  String key,
        @RequestPart("files[]") List<MultipartFile> files
        ) throws ClassNotFoundException, IOException {
        log.debug("REST request to upload attached file");

        MultipartFile f = files.get(0);

        if(filename==null) {
            filename = f.getOriginalFilename();
            if (filename.contains("/")) {
                String[] parts = filename.split("/");
                filename = parts[parts.length-1];
            }
        }
        log.info("Upload {} for domain {} {}", filename, domainClassName, domainIdent);
        log.info("File size = {}", f.getSize());
        AttachedFile attachedFile = attachedFileService.create(filename,f.getBytes(),key,domainIdent,domainClassName);
        return responseSuccess(attachedFile);
    }

    @DeleteMapping("/attachedfile/{id}.json")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        log.debug("REST request to delete attached file {}", id);
        return delete(attachedFileService, JsonObject.of("id", id), null);
    }
}
