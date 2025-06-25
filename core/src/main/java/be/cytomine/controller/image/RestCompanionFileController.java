package be.cytomine.controller.image;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.CompanionFile;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.AbstractImageRepository;
import be.cytomine.service.image.AbstractImageService;
import be.cytomine.service.image.CompanionFileService;
import be.cytomine.service.image.UploadedFileService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestCompanionFileController extends RestCytomineController {

    private final AbstractImageService abstractImageService;

    private final UploadedFileService uploadedFileService;

    private final CompanionFileService companionFileService;

    private final ImageServerService imageServerService;

    private final AbstractImageRepository abstractImageRepository;

    @GetMapping("/abstractimage/{id}/companionfile.json")
    public ResponseEntity<String> listByAbstractImage(
            @PathVariable Long id
    ) {
        log.debug("REST request to list companion file for abstract image {}", id);
        AbstractImage abstractImage = abstractImageRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        return responseSuccess(companionFileService.list(abstractImage));
    }

    @GetMapping("/uploadedfile/{id}/companionfile.json")
    public ResponseEntity<String> listByUploadedFile(
            @PathVariable Long id
    ) {
        log.debug("REST request to list companion file for uploaded file {}", id);
        UploadedFile uploadedFile = uploadedFileService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("UploadedFile", id));
        return responseSuccess(companionFileService.list(uploadedFile));
    }

    @GetMapping("/companionfile/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get companionfile {}", id);
        return companionFileService.find(id)
                .map(this::responseSuccess)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
    }

    @PostMapping("/companionfile.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save companionfile : " + json);
        return add(companionFileService, json);
    }

    @PutMapping("/companionfile/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit companionfile : " + id);
        return update(companionFileService, json);
    }

    @DeleteMapping("/companionfile/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete companionfile : " + id);
        return delete(companionFileService, JsonObject.of("id", id), null);
    }


    @GetMapping("/companionfile/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, ProxyExchange<byte[]> proxy) throws IOException {
        log.debug("REST request to download companionfile");
        CompanionFile companionFile = companionFileService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("CompanionFile", id));
        // TODO: in abstract image, there is no check fos download auth!?
        return imageServerService.download(companionFile, proxy);
    }


    @GetMapping("/companionfile/{id}/user.json")
    public ResponseEntity<String> showUploaderOfImage(@PathVariable Long id) {
        log.debug("REST request to show companionfile uploader");
        CompanionFile companionFile = companionFileService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("CompanionFile", id));
        if (companionFile.getUploadedFile() !=null && companionFile.getUploadedFile().getUser()!=null) {
            return responseSuccess(companionFile.getUploadedFile().getUser());
        } else {
            return responseNotFound("CompanionFile", "User", id);
        }

    }
}
