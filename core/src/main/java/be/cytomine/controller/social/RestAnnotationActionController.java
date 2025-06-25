package be.cytomine.controller.social;

import java.util.Date;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.service.social.AnnotationActionService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.UserService;
import be.cytomine.utils.JsonObject;

/**
 * Controller for user position
 * Position of the user (x,y) on an image for a time
 */
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class RestAnnotationActionController extends RestCytomineController {

    private final AnnotationActionService annotationActionService;

    private final AnnotationDomainRepository annotationDomainRepository;

    private final CurrentUserService currentUserService;

    private final UserService userService;

    private final ImageInstanceService imageInstanceService;

    private final SliceInstanceService sliceInstanceService;

    private final ProjectService projectService;

    @PostMapping("/annotation_action.json")
    public ResponseEntity<String> add(
            @RequestBody JsonObject json
    ) {
        log.debug("REST request add annotation action");
        AnnotationDomain annotationDomain = annotationDomainRepository.findById(json.getJSONAttrLong("annotationIdent", 0L))
                .orElseThrow(() -> new ObjectNotFoundException("Annotation", json.getJSONAttrStr("annotationIdent")));
        return responseSuccess(annotationActionService.add(annotationDomain, currentUserService.getCurrentUser(), json.getJSONAttrStr("action"), new Date()));
    }

    @GetMapping("/imageinstance/{image}/annotation_action.json")
    public ResponseEntity<String> listByImage(
            @PathVariable("image") Long imageId,
            @RequestParam(value = "user", required = false) Long userId,
            @RequestParam(value = "afterThan", required = false) Long afterThan,
            @RequestParam(value = "beforeThan", required = false) Long beforeThan
    ) {
        ImageInstance image = imageInstanceService.find(imageId)
                        .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", imageId));

        User user = null;
        if (userId!=null) {
            user = userService.findUser(userId)
                    .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        }
        return responseSuccess(annotationActionService.list(image, user, afterThan, beforeThan));
    }

    @GetMapping("/sliceinstance/{slice}/annotation_action.json")
    public ResponseEntity<String> listBySlice(
            @PathVariable("slice") Long sliceId,
            @RequestParam(value = "user", required = false) Long userId,
            @RequestParam(value = "afterThan", required = false) Long afterThan,
            @RequestParam(value = "beforeThan", required = false) Long beforeThan
    ) {
        SliceInstance sliceInstance = sliceInstanceService.find(sliceId)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", sliceId));

        User user = null;
        if (userId!=null) {
            user = userService.findUser(userId)
                    .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        }
        return responseSuccess(annotationActionService.list(sliceInstance, user, afterThan, beforeThan));
    }

    @GetMapping("/project/{project}/annotation_action/count.json")
    public ResponseEntity<String> countByProject(
            @PathVariable("project") Long projectId,
            @RequestParam(required = false) Long startDate,
            @RequestParam(required = false) Long endDate
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        return responseSuccess(JsonObject.of("total", annotationActionService.countByProject(project, startDate, endDate)));
    }
}
