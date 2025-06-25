package be.cytomine.controller.ontology;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.image.CropParameter;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.ontology.ReviewedAnnotationService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.report.ReportService;
import be.cytomine.service.security.UserService;
import be.cytomine.service.utils.ParamsService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.AnnotationListingBuilder;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import org.locationtech.jts.io.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestReviewedAnnotationController extends RestCytomineController {

    private final ReviewedAnnotationService reviewedAnnotationService;

    private final ProjectService projectService;

    private final AnnotationListingBuilder annotationListingBuilder;

    private final ReportService reportService;

    private final CurrentUserService currentUserService;

    private final ImageServerService imageServerService;

    private final ParamsService paramsService;

    private final ImageInstanceService imageInstanceService;

    private final TaskService taskService;

    private final UserService userService;

    private final TermService termService;

    @GetMapping("/reviewedannotation.json")
    public ResponseEntity<String> list(
    ) throws IOException {
        log.debug("REST request to list reviewed annotation");
        List<ReviewedAnnotation> annotations = new ArrayList<>();
        for (Project project : projectService.listForCurrentUser()) {
            annotations.addAll(
                    reviewedAnnotationService.list(
                            project,
                            paramsService.getPropertyGroupToShow(
                                    mergeQueryParamsAndBodyParams()
                            )
                    )
            );
        }
        return responseSuccess(annotations);
    }


    @GetMapping("/user/{idUser}/reviewedannotation/count.json")
    public ResponseEntity<String> countByUser(
            @PathVariable(value = "idUser") Long idUser
    ) {
        log.debug("REST request to count reviewed annotation for current user");
        return responseSuccess(JsonObject.of("total", reviewedAnnotationService.count((User)currentUserService.getCurrentUser())));
    }


    @GetMapping("/project/{idProject}/reviewedannotation/count.json")
    public ResponseEntity<String> countByProject(
            @PathVariable(value = "idProject") Long idProject,
            @RequestParam(value="startDate", required = false) Long startDate,
            @RequestParam(value="endDate", required = false) Long endDate
    ) {
        log.debug("REST request to count reviewed annotation by project");
        Project project= projectService.find(idProject)
                .orElseThrow(() -> new ObjectNotFoundException("Project", idProject));
        Date start = (startDate!=null? new Date(startDate) : null);
        Date end = (endDate!=null? new Date(endDate) : null);
        return responseSuccess(JsonObject.of("total", reviewedAnnotationService.countByProject(project, start, end)));
    }


    @GetMapping("/imageinstance/{image}/reviewedannotation/stats.json")
    public ResponseEntity<String> stats(
            @PathVariable(value = "image") Long idImage
    ) {
        log.debug("REST request to list reviewed annotation stats by image");
        ImageInstance imageInstance = imageInstanceService.find(idImage)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", idImage));
        return responseSuccess(reviewedAnnotationService.statsGroupByUser(imageInstance));
    }


    @GetMapping("/reviewedannotation/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get reviewed annotation : {}", id);
        return reviewedAnnotationService.find(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("ReviewedAnnotation", id));
    }

    /**
     * Add reviewed annotation
     * Only use to create a reviewed annotation with all json data.
     * Its better to use 'addAnnotationReview' that needs only the annotation id and a list of term
     */
    @PostMapping("/reviewedannotation.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save reviewed annotation");
        return add(reviewedAnnotationService, json);
    }

    @PutMapping("/reviewedannotation/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit reviewed annotation : " + id);
        return update(reviewedAnnotationService, json);
    }

    @DeleteMapping("/reviewedannotation/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete an annotation : " + id);
        return delete(reviewedAnnotationService, JsonObject.of("id", id), null);
    }


    /**
     * Start the review mode on an image
     * To review annotation, a user must enable review mode in the current image
     */
    @RequestMapping(value = "/imageinstance/{image}/review.json", method = {POST,PUT})
    public ResponseEntity<String> startImageInstanceReview(
            @PathVariable(value = "image") Long idImage
    ) {
        log.debug("REST request to start review of image {}", idImage);
        ImageInstance imageInstance = imageInstanceService.find(idImage)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", idImage));

        imageInstanceService.startReview(imageInstance);

        return responseSuccess(JsonObject.of(
                "message", imageInstance.getReviewUser().getUsername() + " start reviewing on " + imageInstance.getInstanceFilename(),
                "imageinstance", ImageInstance.getDataFromDomain(imageInstance)
        ));
    }

    /**
     * Start the review mode on an image
     * To review annotation, a user must enable review mode in the current image
     */
    @DeleteMapping("/imageinstance/{image}/review.json")
    public ResponseEntity<String> stopImageInstanceReview(
            @PathVariable(value = "image") Long idImage,
            @RequestParam(value = "cancel", defaultValue = "false") Boolean cancel
    ) {
        log.debug("REST request to stop review of image {} with cancel {}",idImage , cancel);
        ImageInstance imageInstance = imageInstanceService.find(idImage)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", idImage));

        imageInstanceService.stopReview(imageInstance, cancel);

        String message = cancel ?
                currentUserService.getCurrentUsername() + " cancel reviewing on " + imageInstance.getInstanceFilename() :
                currentUserService.getCurrentUsername() + " validate reviewing on " + imageInstance.getInstanceFilename();

        return responseSuccess(JsonObject.of(
                "message", message,
                "imageinstance", ImageInstance.getDataFromDomain(imageInstance)
        ));
    }


    @RequestMapping(value = "/annotation/{annotation}/review.json", method = {GET, POST,PUT})
    public ResponseEntity<String> addAnnotationReview(
            @PathVariable(value = "annotation") Long idAnnotation
    ) throws IOException {
        log.debug("REST request to create review of annotation {}", idAnnotation);
        JsonObject params = mergeQueryParamsAndBodyParams();


        CommandResponse response = reviewedAnnotationService.reviewAnnotation(idAnnotation, params.getJSONAttrListLong("terms", null));

        return responseSuccess(response);
    }


    @DeleteMapping(value = "/annotation/{annotation}/review.json")
    public ResponseEntity<String> deleteAnnotationReview(
            @PathVariable(value = "annotation") Long idAnnotation
    ) throws IOException {
        log.debug("REST request to create review of annotation {}", idAnnotation);

        CommandResponse response = reviewedAnnotationService.unReviewAnnotation(idAnnotation);

        return responseSuccess(response);
    }


    /**
     * Review all annotation in image for a user
     * It support the task functionnality, if task param is set,
     * this method will update its progress status to the task.
     * User can access task status by getting the task info
     */
    @RequestMapping(value = "/imageinstance/{image}/annotation/review.json", method = {POST,PUT})
    public ResponseEntity<String> reviewLayer(
            @PathVariable(value = "image") Long idImage
    ) throws IOException {
        log.debug("REST request to review layer of image {}", idImage);
        JsonObject params = mergeQueryParamsAndBodyParams();

        Task task = taskService.get(params.getJSONAttrLong("task", -1L));
        return responseSuccess(reviewedAnnotationService.reviewLayer(idImage, params.getJSONAttrListLong("users"), task));
    }

    /**
     * Unreview all annotation for all layers in params
     */
    @RequestMapping(value = "/imageinstance/{image}/annotation/review.json", method = {DELETE})
    public ResponseEntity<String> unReviewLayer(
            @PathVariable(value = "image") Long idImage
    ) throws IOException {
        log.debug("REST request to un review layer of image {}", idImage);
        JsonObject params = mergeQueryParamsAndBodyParams();

        Task task = taskService.get(params.getJSONAttrLong("task", -1L));
        return responseSuccess(reviewedAnnotationService.unreviewLayer(idImage, params.getJSONAttrListLong("users"), task));
    }


    /**
     * Download a report (pdf, xls,...) with reviewed annotation data from a specific project
     */
    @GetMapping("/project/{idProject}/reviewedannotation/download")
    public void downloadDocumentByProject(
            @PathVariable Long idProject,
            @RequestParam String format,
            @RequestParam String terms,
            @RequestParam String reviewUsers,
            @RequestParam String images,
            @RequestParam(required = false) Long beforeThan,
            @RequestParam(required = false) Long afterThan
    ) throws IOException {
        Project project = projectService.find(idProject)
                .orElseThrow(() -> new ObjectNotFoundException("Project", idProject));
        reviewUsers = userService.fillEmptyUserIds(reviewUsers, idProject);
        terms = termService.fillEmptyTermIds(terms, project);
        JsonObject params = mergeQueryParamsAndBodyParams();
        params.put("reviewed", true);
        byte[] report = annotationListingBuilder.buildAnnotationReport(idProject, reviewUsers, params, terms, format);
        responseReportFile(reportService.getAnnotationReportFileName(format, idProject), report, format);
    }

    @RequestMapping(value = "/reviewedannotation/{id}/crop.{format}", method = {GET, POST})
    public ResponseEntity<byte[]> crop(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(required = false) Integer maxSize,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String boundaries,
            @RequestParam(defaultValue = "false") Boolean complete,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(required = false) Double increaseArea,
            @RequestParam(required = false) Boolean safe,
            @RequestParam(required = false) Boolean square,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean draw,
            @RequestParam(required = false) Boolean mask,
            @RequestParam(required = false) Boolean alphaMask,
            @RequestParam(required = false) Boolean drawScaleBar,
            @RequestParam(required = false) Double resolution,
            @RequestParam(required = false) Double magnification,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits,
            @RequestParam(required = false) Integer alpha,
            @RequestParam(required = false) Integer thickness,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer jpegQuality,

            ProxyExchange<byte[]> proxy
    ) throws IOException, ParseException {
        log.debug("REST request to get associated image of a abstract image");
        ReviewedAnnotation reviewedannotation = reviewedAnnotationService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ReviewedAnnotation", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
        cropParameter.setComplete(complete);
        cropParameter.setZoom(zoom);
        cropParameter.setMaxSize(maxSize);
        cropParameter.setIncreaseArea(increaseArea);
        cropParameter.setSafe(safe);
        cropParameter.setSquare(square);
        cropParameter.setType(type);
        cropParameter.setDraw(draw);
        cropParameter.setMask(mask);
        cropParameter.setAlphaMask(alphaMask);
        cropParameter.setDrawScaleBar(drawScaleBar);
        cropParameter.setResolution(resolution);
        cropParameter.setMagnification(magnification);
        cropParameter.setColormap(colormap);
        cropParameter.setInverse(inverse);
        cropParameter.setGamma(gamma);
        cropParameter.setAlpha(alpha);
        cropParameter.setContrast(contrast);
        cropParameter.setThickness(thickness);
        cropParameter.setColor(color);
        cropParameter.setJpegQuality(jpegQuality);
        cropParameter.setMaxBits(bits!=null && bits.equals("max"));
        cropParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        cropParameter.setFormat(format);
        String etag = getRequestETag();
        return imageServerService.crop(reviewedannotation, cropParameter, etag, proxy);
    }

    @RequestMapping(value = "/reviewedannotation/{id}/mask.{format}", method = {GET, POST})
    public ResponseEntity<byte[]> cropMask(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(required = false) Integer maxSize,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String boundaries,
            @RequestParam(defaultValue = "false") Boolean complete,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(required = false) Double increaseArea,
            @RequestParam(required = false) Boolean safe,
            @RequestParam(required = false) Boolean square,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean draw,
            @RequestParam(required = false) Boolean drawScaleBar,
            @RequestParam(required = false) Double resolution,
            @RequestParam(required = false) Double magnification,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits,
            @RequestParam(required = false) Integer alpha,
            @RequestParam(required = false) Integer thickness,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer jpegQuality,

            ProxyExchange<byte[]> proxy
    ) throws IOException, ParseException {
        log.debug("REST request to get associated image of a abstract image");
        ReviewedAnnotation reviewedannotation = reviewedAnnotationService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ReviewedAnnotation", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
        cropParameter.setComplete(complete);
        cropParameter.setZoom(zoom);
        cropParameter.setMaxSize(maxSize);
        cropParameter.setIncreaseArea(increaseArea);
        cropParameter.setSafe(safe);
        cropParameter.setSquare(square);
        cropParameter.setType(type);
        cropParameter.setDraw(draw);
        cropParameter.setMask(true);
        cropParameter.setAlphaMask(false);
        cropParameter.setDrawScaleBar(drawScaleBar);
        cropParameter.setResolution(resolution);
        cropParameter.setMagnification(magnification);
        cropParameter.setColormap(colormap);
        cropParameter.setInverse(inverse);
        cropParameter.setGamma(gamma);
        cropParameter.setAlpha(alpha);
        cropParameter.setContrast(contrast);
        cropParameter.setThickness(thickness);
        cropParameter.setColor(color);
        cropParameter.setJpegQuality(jpegQuality);
        cropParameter.setMaxBits(bits!=null && bits.equals("max"));
        cropParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        cropParameter.setFormat(format);
        String etag = getRequestETag();
        return imageServerService.crop(reviewedannotation, cropParameter, etag, proxy);
    }

    @RequestMapping(value = "/reviewedannotation/{id}/alphamask.{format}", method = {GET, POST})
    public ResponseEntity<byte[]> cropAlphaMask(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(required = false) Integer maxSize,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String boundaries,
            @RequestParam(defaultValue = "false") Boolean complete,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(required = false) Double increaseArea,
            @RequestParam(required = false) Boolean safe,
            @RequestParam(required = false) Boolean square,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean draw,
            @RequestParam(required = false) Boolean drawScaleBar,
            @RequestParam(required = false) Double resolution,
            @RequestParam(required = false) Double magnification,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits,
            @RequestParam(required = false) Integer alpha,
            @RequestParam(required = false) Integer thickness,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer jpegQuality,

            ProxyExchange<byte[]> proxy
    ) throws IOException, ParseException {
        log.debug("REST request to get associated image of a abstract image");
        ReviewedAnnotation reviewedannotation = reviewedAnnotationService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ReviewedAnnotation", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
        cropParameter.setComplete(complete);
        cropParameter.setZoom(zoom);
        cropParameter.setMaxSize(maxSize);
        cropParameter.setIncreaseArea(increaseArea);
        cropParameter.setSafe(safe);
        cropParameter.setSquare(square);
        cropParameter.setType(type);
        cropParameter.setDraw(draw);
        cropParameter.setAlphaMask(true);
        cropParameter.setDrawScaleBar(drawScaleBar);
        cropParameter.setResolution(resolution);
        cropParameter.setMagnification(magnification);
        cropParameter.setColormap(colormap);
        cropParameter.setInverse(inverse);
        cropParameter.setGamma(gamma);
        cropParameter.setAlpha(alpha);
        cropParameter.setContrast(contrast);
        cropParameter.setThickness(thickness);
        cropParameter.setColor(color);
        cropParameter.setJpegQuality(jpegQuality);
        cropParameter.setMaxBits(bits!=null && bits.equals("max"));
        cropParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        cropParameter.setFormat(format);
        String etag = getRequestETag();
        return imageServerService.crop(reviewedannotation, cropParameter, etag, proxy);
    }
}
