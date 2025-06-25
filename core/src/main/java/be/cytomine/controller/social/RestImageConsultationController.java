package be.cytomine.controller.social;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.report.ReportService;
import be.cytomine.service.security.UserService;
import be.cytomine.service.social.ImageConsultationService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.RequestParams;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class RestImageConsultationController extends RestCytomineController {

    private final ImageConsultationService imageConsultationService;

    private final CurrentUserService currentUserService;

    private final ProjectService projectService;

    private final UserService userService;

    private final ReportService reportService;

    @PostMapping("/imageinstance/{id}/consultation.json")
    public ResponseEntity<String> add(
            @PathVariable("id") Long imageId,
            @RequestBody JsonObject json
    ) {
        log.info("add an image consultation for image {}", imageId);
        User user = currentUserService.getCurrentUser();
        String session = RequestContextHolder.currentRequestAttributes().getSessionId();
        String mode = json.getJSONAttrStr("mode");
        return responseSuccess(imageConsultationService.add(user, imageId, session, mode, new Date()));
    }

    @GetMapping("/project/{project}/lastImages.json")
    public ResponseEntity<String> lastImageOfUsersByProject(
            @PathVariable("project") Long projectId
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(imageConsultationService.lastImageOfUsersByProject(project, null, "created", "desc", 0L, 0L));
    }

    @GetMapping("/imageinstance/method/lastopened.json")
    public ResponseEntity<String> listLastOpenImage() {
        log.debug("REST request to get last image instance opened for user");
        RequestParams requestParams = retrievePageableParameters();
//        User secUser = currentUserService.getCurrentUser();
        return responseSuccess(imageConsultationService.listLastOpened(requestParams.getMax()));
    }

    @GetMapping("/project/{project}/user/{user}/imageconsultation.json")
    public ResponseEntity<String> listImageConsultationByProjectAndUser(
            @PathVariable("project") Long projectId,
            @PathVariable("user") Long userId,
            @RequestParam(required = false, defaultValue = "false") Boolean distinctImages,
            @RequestParam(required = false, defaultValue = "0") Integer max,
            @RequestParam(required = false, defaultValue = "0") Integer offset
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        User user = userService.find(userId).orElseThrow(() -> new ObjectNotFoundException("User", userId));

        if (distinctImages) {
            return responseSuccess(imageConsultationService.listImageConsultationByProjectAndUserWithDistinctImage(project, user));
        } else {
            return responseSuccess(imageConsultationService.listImageConsultationByProjectAndUserNoImageDistinct(project, user, max, offset));
        }
    }

    @GetMapping("/project/{project}/imageconsultation/count.json")
    public ResponseEntity<String> countByProject(
            @PathVariable("project") Long projectId,
            @RequestParam(required = false) Long startDate,
            @RequestParam(required = false) Long endDate
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        return responseSuccess(JsonObject.of("total", imageConsultationService.countByProject(project, startDate, endDate)));
    }

    @GetMapping("/imageconsultation/resume.json")
    public ResponseEntity<String> resumeByUserAndProject(
            @RequestParam(value = "user") Long userId,
            @RequestParam(value = "project") Long projectId,
            @RequestParam(required = false, value = "export") String export
    ) throws IOException {
        List<JsonObject> results = imageConsultationService.resumeByUserAndProject(userId, projectId);
        if (export!=null && export.equals("csv")) {

            Project project = projectService.find(projectId)
                    .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
            User user = userService.findUser(userId)
                    .orElseThrow(() -> new ObjectNotFoundException("User", userId));

            byte[] report = reportService.generateImageConsultationReport(project.getName(), user.getUsername(), results);
            responseReportFile(reportService.getImageConsultationReportFileName(export, projectId, userId), report, export);
            return null;
        } else {
            return responseSuccess(results);
        }
    }
}
