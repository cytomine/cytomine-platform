package be.cytomine.controller.stats;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecRoleService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.service.stats.StatsService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

import static org.springframework.security.acls.domain.BasePermission.READ;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestStatsController extends RestCytomineController {

    private final StatsService statsService;

    private final ProjectService projectService;

    private final SecurityACLService securityACLService;

    private final CurrentUserService currentUserService;

    private final TermRepository termRepository;

    private final ProjectConnectionService projectConnectionService;

    @GetMapping("/project/{project}/stats/term.json")
    public ResponseEntity<String> statTerm(
            @PathVariable("project") Long projectId,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong,
            @RequestParam(required = false, defaultValue = "true") Boolean leafOnly
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;
        return responseSuccess(statsService.statTerm(project, startDate, endDate, leafOnly));
    }

    @GetMapping("/project/{project}/stats/user.json")
    public ResponseEntity<String> statUser(
            @PathVariable("project") Long projectId,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;
        return responseSuccess(statsService.statUser(project, startDate, endDate));
    }

    @GetMapping("/project/{project}/stats/termslide.json")
    public ResponseEntity<String> statTermSlide(
            @PathVariable("project") Long projectId,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;
        return responseSuccess(statsService.statTermSlide(project, startDate, endDate));
    }


    @GetMapping("/project/{project}/stats/termimage.json")
    public ResponseEntity<String> statPerTermAndImage(
            @PathVariable("project") Long projectId,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;
        return responseSuccess(statsService.statPerTermAndImage(project, startDate, endDate));
    }


    @GetMapping("/project/{project}/stats/userslide.json")
    public ResponseEntity<String> statUserSlide(
            @PathVariable("project") Long projectId,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;
        return responseSuccess(statsService.statUserSlide(project, startDate, endDate));
    }


    @GetMapping("/project/{project}/stats/userannotations.json")
    public ResponseEntity<String> statUserAnnotations(
            @PathVariable("project") Long projectId
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(statsService.statUserAnnotations(project));
    }

    @GetMapping("/project/{project}/stats/annotationevolution.json")
    public ResponseEntity<String> statAnnotationEvolution(
            @PathVariable("project") Long projectId,
            @RequestParam(required = false, defaultValue = "1") Integer daysRange,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong,
            @RequestParam(value = "term", required = false) Long termId,
            @RequestParam(value = "accumulate", required = false, defaultValue = "true") Boolean accumulate,
            @RequestParam(value = "reverseOrder", required = false, defaultValue = "true") Boolean reverseOrder


    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        Term term = null;
        if (termId !=null) {
            term = termRepository.findById(termId)
                    .orElseThrow(() -> new ObjectNotFoundException("Term", termId));
        }

        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;

        return responseSuccess(statsService.statAnnotationEvolution(project, term, daysRange, startDate, endDate, reverseOrder, accumulate));
    }

    @GetMapping("/project/{project}/stats/reviewedannotationevolution.json")
    public ResponseEntity<String> statReviewedAnnotationEvolution(
            @PathVariable("project") Long projectId,
            @RequestParam(required = false, defaultValue = "1") Integer daysRange,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong,
            @RequestParam(value = "term", required = false) Long termId,
            @RequestParam(value = "accumulate", required = false, defaultValue = "true") Boolean accumulate,
            @RequestParam(value = "reverseOrder", required = false, defaultValue = "true") Boolean reverseOrder


    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        Term term = null;
        if (termId !=null) {
            term = termRepository.findById(termId)
                    .orElseThrow(() -> new ObjectNotFoundException("Term", termId));
        }

        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;

        return responseSuccess(statsService.statReviewedAnnotationEvolution(project, term, daysRange, startDate, endDate, reverseOrder, accumulate));
    }



    @GetMapping("/term/{id}/project/stat.json")
    public ResponseEntity<String> statAnnotationTermedByProject(
            @PathVariable("id") Long id
    ) {

        Term term = termRepository.findById(id)
                    .orElseThrow(() -> new ObjectNotFoundException("Term", id));
        securityACLService.check(term.container(),READ);
        return responseSuccess(statsService.statAnnotationTermedByProject(term));
    }


    @GetMapping("/total/project/connections.json")
    public ResponseEntity<String> totalNumberOfConnectionsByProject(
    ) {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return responseSuccess(projectConnectionService.totalNumberOfConnectionsByProject());
    }

    @GetMapping("/total/{domain}.json")
    public ResponseEntity<String> totalDomains(
            @PathVariable String domain
    ) {
        try {
            return responseSuccess(JsonObject.of("total", statsService.total(Class.forName(domain))));
        } catch (ClassNotFoundException e) {
            throw new ObjectNotFoundException("Class", domain);
        }
    }

    @GetMapping("/stats/currentStats.json")
    public ResponseEntity<String> statsOfCurrentActions(
    ) {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        JsonObject result = new JsonObject();
        result.put("users", statsService.numberOfCurrentUsers());
        result.put("projects", statsService.numberOfActiveProjects());
        result.put("mostActiveProject", statsService.mostActiveProjects().orElse(null));
        return responseSuccess(result);
    }


    @GetMapping("/stats/imageserver/total.json")
    public ResponseEntity<String> statUsedStorage() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return responseSuccess(statsService.statUsedStorage());
    }

    @GetMapping("/project/{project}/stats/connectionsevolution.json")
    public ResponseEntity<String> statConnectionsEvolution(
            @PathVariable(value = "project") Long projectId,
            @RequestParam(required = false, defaultValue = "1") Integer daysRange,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong,
            @RequestParam(value = "accumulate", required = false, defaultValue = "true") Boolean accumulate
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        securityACLService.check(project, READ);

        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;

        return responseSuccess(statsService.statConnectionsEvolution(project, daysRange, startDate, endDate, accumulate));
    }


    @GetMapping("/project/{project}/stats/imageconsultationsevolution.json")
    public ResponseEntity<String> statImageConsultationsEvolution(
            @PathVariable(value = "project") Long projectId,
            @RequestParam(required = false, defaultValue = "1") Integer daysRange,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong,
            @RequestParam(value = "accumulate", required = false, defaultValue = "true") Boolean accumulate
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        securityACLService.check(project, READ);

        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;

        return responseSuccess(statsService.statImageConsultationsEvolution(project, daysRange, startDate, endDate, accumulate));
    }




    @GetMapping("/project/{project}/stats/annotationactionsevolution.json")
    public ResponseEntity<String> statAnnotationActionsEvolution(
            @PathVariable(value = "project") Long projectId,
            @RequestParam(required = false, defaultValue = "1") Integer daysRange,
            @RequestParam(value = "startDate", required = false) Long startDateLong,
            @RequestParam(value = "endDate", required = false) Long endDateLong,
            @RequestParam(value = "accumulate", required = false, defaultValue = "true") Boolean accumulate,
            @RequestParam(required = false) String type
    ) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        securityACLService.check(project, READ);

        Date startDate = startDateLong != null ? new Date(startDateLong) : null;
        Date endDate = endDateLong != null ? new Date(endDateLong) : null;

        return responseSuccess(statsService.statAnnotationActionsEvolution(project, daysRange, startDate, endDate, accumulate, type));
    }

    @GetMapping("/stats/all.json")
    public ResponseEntity<String> allGlobalStats() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        JsonObject result = new JsonObject();
        result.put("users", statsService.total(User.class));
        result.put("projects", statsService.total(Project.class));
        result.put("images", statsService.total(ImageInstance.class));
        result.put("abstractImages", statsService.total(AbstractImage.class));
        result.put("userAnnotations", statsService.total(UserAnnotation.class));
        result.put("jobAnnotations", 0);
        result.put("terms", statsService.total(Term.class));
        result.put("ontologies", statsService.total(Ontology.class));

        return responseSuccess(result);
    }
}
