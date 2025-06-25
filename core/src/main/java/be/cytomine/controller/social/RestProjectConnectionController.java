package be.cytomine.controller.social;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.PersistentImageConsultation;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.report.ReportService;
import be.cytomine.service.security.UserService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestProjectConnectionController extends RestCytomineController {

    private final ProjectConnectionService projectConnectionService;

    private final CurrentUserService currentUserService;

    private final ProjectService projectService;

    private final UserService userService;

    private final ReportService reportService;

    @PostMapping("/project/{project}/userconnection.json")
    public ResponseEntity<String> add(
            @PathVariable("project") Long projectId,
            @RequestBody JsonObject json
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        String session = RequestContextHolder.currentRequestAttributes().getSessionId();
        String os = json.getJSONAttrStr("os");
        String browser = json.getJSONAttrStr("browser");
        String browserVersion = json.getJSONAttrStr("browserVersion");
        return responseSuccess(projectConnectionService.add(currentUserService.getCurrentUser(), project, session, os, browser, browserVersion));

    }

    @GetMapping("/project/{project}/lastConnection.json")
    public ResponseEntity<String> lastConnectionInProject(
            @PathVariable("project") Long projectId
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(projectConnectionService.lastConnectionInProject(project, null, "created", "desc", 0L, 0L));

    }

    @GetMapping("/project/{project}/lastConnection/{user}.json")
    public ResponseEntity<String> lastConnectionInProjectByUser(
            @PathVariable("project") Long projectId,
            @PathVariable("user") Long userId
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(projectConnectionService.lastConnectionInProject(project, List.of(userId), "created", "desc", 0L, 0L));

    }

    @GetMapping("/project/{project}/userconnection/{user}.json")
    public ResponseEntity<String> getConnectionByUserAndProject(
            @PathVariable("project") Long projectId,
            @PathVariable("user") Long userId,
            @RequestParam(required = false, defaultValue = "0") Integer max,
            @RequestParam(required = false, defaultValue = "0") Integer offset
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));

        return responseSuccess(projectConnectionService.getConnectionByUserAndProject(user, project, max, offset));

    }

    @GetMapping("/project/{project}/connectionFrequency.json")
    public ResponseEntity<String> numberOfConnectionsByProject(
            @PathVariable("project") Long projectId,
            @RequestParam(required = false) Long afterThan,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "false") Boolean heatmap
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        if (heatmap) {
            return responseSuccess(projectConnectionService.numberOfConnectionsByProjectOrderedByHourAndDays(project, afterThan, null));
        } else if(period!=null) {
            return responseSuccess(projectConnectionService.numberOfProjectConnections(period, afterThan, null, project, null));
        } else {
            return responseSuccess(projectConnectionService.numberOfConnectionsByProjectAndUser(project, null, "created", "desc", 0L, 0L));
        }
    }



    @GetMapping("/project/{project}/connectionFrequency/{user}.json")
    public ResponseEntity<String> numberOfConnectionsByProjectAndUser(
            @PathVariable("project") Long projectId,
            @PathVariable("user") Long userId,
            @RequestParam(required = false) Long afterThan,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "false") Boolean heatmap
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        if (heatmap) {
            return responseSuccess(projectConnectionService.numberOfConnectionsByProjectOrderedByHourAndDays(project, afterThan, user));
        } else if(period!=null) {
            return responseSuccess(projectConnectionService.numberOfProjectConnections(period, afterThan, null, project, user));
        } else {
            return responseSuccess(projectConnectionService.numberOfConnectionsByProjectAndUser(project, List.of(user.getId()), "created", "desc", 0L, 0L));
        }
    }


    @GetMapping("/connectionFrequency.json")
    public ResponseEntity<String> lastConnectionInProjectByUser(
            @RequestParam(required = false) Long afterThan,
            @RequestParam(required = false) Long beforeThan,
            @RequestParam(required = true) String period
    ) {
        return responseSuccess(projectConnectionService.numberOfProjectConnections(period, afterThan, beforeThan, null, null));
    }

    @GetMapping("/averageConnections.json")
    public ResponseEntity<String> averageOfProjectConnections(
            @RequestParam(required = false) Long afterThan,
            @RequestParam(required = false) Long beforeThan,
            @RequestParam(required = true) String period,
            @RequestParam(required = false, value = "project", defaultValue = "0") Long projectId,
            @RequestParam(required = false, value = "user", defaultValue = "0") Long userId
    ) {
        Project project = projectService.find(projectId).orElse(null);
        User user = userService.find(userId).orElse(null);
        return responseSuccess(projectConnectionService.averageOfProjectConnections(period, afterThan, beforeThan, project, user));
    }


    @GetMapping("/project/{project}/userconnection/count.json")
    public ResponseEntity<String> countByProject(
            @PathVariable("project") Long projectId,
            @RequestParam(required = false) Long startDate,
            @RequestParam(required = false) Long endDate
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(JsonObject.of("total", projectConnectionService.countByProject(project, startDate, endDate)));
    }

    @GetMapping("/project/{project}/connectionHistory/{user}.json")
    public ResponseEntity<String> userProjectConnectionHistory(
            @PathVariable("project") Long projectId,
            @PathVariable("user") Long userId,
            @RequestParam(required = false, defaultValue = "0") Integer max,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, value = "export") String export
    ) throws IOException {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));

        Page<PersistentProjectConnection> page = projectConnectionService.getConnectionByUserAndProject(user, project, max, offset);

        if (export!=null && export.equals("csv")) {

            List<JsonObject> projectConnectionDataList = new ArrayList<>();
            for(PersistentProjectConnection projectConnection : page){
                projectConnectionDataList.add(PersistentProjectConnection.getDataFromDomain(projectConnection));
            }

            byte[] report = reportService.generateConnectionHistoryReport(project.getName(), user.getUsername(), projectConnectionDataList);
            responseReportFile(reportService.getConnectionHistoryReportFileName(export, projectId, userId), report, export);
            return null;
        } else {
            return responseSuccess(page);
        }

    }

    @GetMapping("/projectConnection/{id}.json")
    public ResponseEntity<String> getUserActivityDetails(
            @PathVariable("id") Long id,
            @RequestParam(required = false, defaultValue = "0", value = "start") Integer max, // why not max?
            @RequestParam(required = false, defaultValue = "0", value = "length") Integer offset,  // why not length?
            @RequestParam(required = false) String export
            ) {

        List<PersistentImageConsultation> result = projectConnectionService.getUserActivityDetails(id);

        if (export!=null && export.equals("csv")) {
            throw new CytomineMethodNotYetImplementedException("todo");
        } else {
            return responseSuccess(result, offset, max, false);
        }

    }
}
