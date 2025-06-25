package be.cytomine.controller.processing;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.processing.ImageFilterRepository;
import be.cytomine.service.processing.ImageFilterProjectService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.utils.JsonObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageFilterController extends RestCytomineController {

    private final ImageFilterRepository imageFilterRepository;

    @GetMapping("/imagefilter.json")
    public ResponseEntity<String> list(
    ) {
        log.debug("REST request to list imagefilterproject");
        return responseSuccess(imageFilterRepository.findAll());
    }
}
