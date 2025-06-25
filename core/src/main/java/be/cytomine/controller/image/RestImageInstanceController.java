package be.cytomine.controller.image;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.image.group.ImageGroupImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.image.CropParameter;
import be.cytomine.dto.image.ImageParameter;
import be.cytomine.dto.image.LabelParameter;
import be.cytomine.dto.image.WindowParameter;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.image.AbstractImageService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.SliceCoordinatesService;
import be.cytomine.service.image.group.ImageGroupImageInstanceService;
import be.cytomine.service.image.group.ImageGroupService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.search.ImageSearchExtension;
import be.cytomine.service.security.UserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.RequestParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.io.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.cloud.gateway.mvc.ProxyExchange;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestImageInstanceController extends RestCytomineController {

    private final AbstractImageService abstractImageService;

    private final ProjectService projectService;

    private final ImageInstanceService imageInstanceService;

    private final ImageGroupService imageGroupService;

    private final ImageGroupImageInstanceService imageGroupImageInstanceService;

    private final ImageServerService imageServerService;

    private final UserService userService;
    
    private final SliceCoordinatesService sliceCoordinatesService;

    private final SecurityACLService securityACLService;

    private final CurrentUserService currentUserService;


    @GetMapping("/imageinstance/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance {}", id);

        return imageInstanceService.find(id)
                .map(x -> responseSuccess(x, securityACLService.isFilterRequired(x.getProject())))
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
    }

    @GetMapping("/user/{id}/imageinstance.json")
    public ResponseEntity<String> listByUser(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance by user {}", id);
        User user = userService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("User", id));
        RequestParams requestParams = retrievePageableParameters();
        return responseSuccess(imageInstanceService.list(user, retrieveSearchParameters(), requestParams.getSort(), requestParams.getOrder(), requestParams.getOffset(), requestParams.getMax()));
    }

    @GetMapping("/user/{id}/imageinstance/light.json")
    public ResponseEntity<String> listLightByUser(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance light by user {}", id);
        User currentUser = currentUserService.getCurrentUser();
        if (id != 0) {
            currentUser = userService.find(id)
                    .orElseThrow(() -> new ObjectNotFoundException("User", id));
        }
        return responseSuccess(imageInstanceService.listLight(currentUser));
    }

    @GetMapping("/abstractimage/{id}/imageinstance.json")
    public ResponseEntity<String> listByAbstractImage(@PathVariable Long id) {
        log.debug("REST request to list images for abstract image {}", id);

        AbstractImage ai = abstractImageService.find(id)
            .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));

        return responseSuccess(imageInstanceService.listByAbstractImage(ai));
    }

    @GetMapping("/project/{id}/imageinstance.json")
    public ResponseEntity<String> listByProject(
            @PathVariable Long id,
            @RequestParam(value = "light", defaultValue = "false", required = false) Boolean light,
            @RequestParam(value = "tree", defaultValue = "false", required = false) Boolean tree,
            @RequestParam(value = "withLastActivity", defaultValue = "false", required = false) Boolean withLastActivity,
            @RequestParam(value = "sort", defaultValue = "created", required = false) String sort,
            @RequestParam(value = "order", defaultValue = "desc", required = false) String order,
            @RequestParam(value = "offset", defaultValue = "0", required = false) Integer offset,
            @RequestParam(value = "max", defaultValue = "0", required = false) Integer max

    ) {
        log.debug("REST request to list images for project : {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
        RequestParams requestParams = retrievePageableParameters();
        if (light) {
            return responseSuccess(imageInstanceService.listLight(project), securityACLService.isFilterRequired(project));
        } else if (tree) {
            return responseSuccess(imageInstanceService.listTree(project, requestParams.getOffset(), requestParams.getMax()), securityACLService.isFilterRequired(project));
        } else if (withLastActivity) {
            ImageSearchExtension imageSearchExtension = new ImageSearchExtension();
            imageSearchExtension.setWithLastActivity(withLastActivity);
            return responseSuccess(imageInstanceService.listExtended(project, imageSearchExtension, retrieveSearchParameters(), requestParams.getSort(), requestParams.getOrder(), requestParams.getOffset(), requestParams.getMax()), securityACLService.isFilterRequired(project));
        } else {
            return responseSuccess(
                imageInstanceService.list(
                    project,
                    retrieveSearchParameters(),
                    requestParams.getSort(),
                    requestParams.getOrder(),
                    requestParams.getOffset(),
                    requestParams.getMax(),
                    false,
                    requestParams.getWithImageGroup()
                ),
                securityACLService.isFilterRequired(project)
            );
        }
    }

    @GetMapping("/imageinstance/{id}/next.json")
    public ResponseEntity<String> next(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance {}", id);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        return imageInstanceService.next(imageInstance)
                .map(x -> responseSuccess(x, securityACLService.isFilterRequired(x.getProject())))
                .orElseGet(() -> responseSuccess(new JsonObject()));
    }

    @GetMapping("/imageinstance/{id}/previous.json")
    public ResponseEntity<String> previous(
            @PathVariable Long id
    ) {
        log.debug("REST request to get image instance {}", id);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        return imageInstanceService.previous(imageInstance)
                .map(x -> responseSuccess(x, securityACLService.isFilterRequired(x.getProject())))
                .orElseGet(() -> responseSuccess(new JsonObject()));
    }

    @PostMapping("/imageinstance.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save imageinstance : " + json);
        return add(imageInstanceService, json);
    }

    @PutMapping("/imageinstance/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit imageinstance : " + id);
        return update(imageInstanceService, json);
    }

    @DeleteMapping("/imageinstance/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete imageinstance : " + id);
        return delete(imageInstanceService, JsonObject.of("id", id), null);
    }

    //    // TODO:MIGRATION GET params vs POST params!
    @RequestMapping(value = "/imageinstance/{id}/thumb.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> thumb(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "512", required = false) Integer maxSize,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits,
            @RequestParam(required = false) String Authorization,

            ProxyExchange<byte[]> proxy
    ) throws IOException {
        log.debug("REST request get imageinstance {} thumb {}", id, format);
        ImageParameter thumbParameter = new ImageParameter();
        thumbParameter.setFormat(format);
        thumbParameter.setMaxSize(maxSize);
        thumbParameter.setColormap(colormap);
        thumbParameter.setInverse(inverse);
        thumbParameter.setContrast(contrast);
        thumbParameter.setGamma(gamma);
        thumbParameter.setMaxBits(bits!=null && bits.equals("max"));
        thumbParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        ImageInstance imageInstance = imageInstanceService.find(id, Authorization)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        String etag = getRequestETag();
        return imageServerService.thumb(imageInstance, thumbParameter, etag, proxy);
    }


    @RequestMapping(value = "/imageinstance/{id}/preview.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> preview(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "1024", required = false) Integer maxSize,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits,

            ProxyExchange<byte[]> proxy
    ) throws IOException {
        log.debug("REST request get imageInstance {} preview {}", id, format);
        ImageParameter previewParameter = new ImageParameter();
        previewParameter.setFormat(format);
        previewParameter.setMaxSize(maxSize);
        previewParameter.setColormap(colormap);
        previewParameter.setInverse(inverse);
        previewParameter.setContrast(contrast);
        previewParameter.setGamma(gamma);
        previewParameter.setMaxBits(bits != null && bits.equals("max"));
        previewParameter.setBits(bits != null && !bits.equals("max") ? Integer.parseInt(bits) : null);

        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        String etag = getRequestETag();
        return imageServerService.thumb(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), previewParameter, etag, proxy);
    }


    @GetMapping("/imageinstance/{id}/associated.json")
    public ResponseEntity<String> associated(@PathVariable Long id) throws IOException {
        log.debug("REST request to get available associated images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.associated(imageInstance));
    }


    @GetMapping("/imageinstance/{id}/histogram.json")
    public ResponseEntity<String> histogram(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "256") Integer nBins) throws IOException {
        log.debug("REST request to get histogram images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.imageHistogram(imageInstance.getBaseImage(), nBins));
    }

    @GetMapping("/imageinstance/{id}/histogram/bounds.json")
    public ResponseEntity<String> histogramBounds(@PathVariable Long id) throws IOException {
        log.debug("REST request to get bounds images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.imageHistogramBounds(imageInstance.getBaseImage()));
    }

    @GetMapping("/imageinstance/{id}/channelhistogram.json")
    public ResponseEntity<String> channelHistograms(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "256") Integer nBins) throws IOException {
        log.debug("REST request to get channelhistogram images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.channelHistograms(imageInstance.getBaseImage(), nBins));
    }


    @GetMapping("/imageinstance/{id}/channelhistogram/bounds.json")
    public ResponseEntity<String> channelHistogramBounds(@PathVariable Long id) throws IOException {
        log.debug("REST request to get channelHistogramBounds images");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(imageServerService.channelHistogramBounds(imageInstance.getBaseImage()));
    }


    @RequestMapping(value = "/imageinstance/{id}/associated/{label}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> label(
            @PathVariable Long id,
            @PathVariable String label,
            @PathVariable String format,
            @RequestParam(defaultValue = "256") Integer maxSize,

            ProxyExchange<byte[]> proxy) throws IOException {
        log.debug("REST request to get associated image of an imageInstance image");
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        if(securityACLService.isFilterRequired(imageInstance.getProject())){
            throw new ForbiddenException("You don't have the right to read or modify this resource! "  + imageInstance.getClass().toString() + " " + id);
        }
        LabelParameter labelParameter = new LabelParameter();
        labelParameter.setFormat(format);
        labelParameter.setLabel(label);
        labelParameter.setMaxSize(maxSize);
        String etag = getRequestETag();
        return imageServerService.label(imageInstance, labelParameter, etag, proxy);
    }

    @RequestMapping(value = "/imageinstance/{id}/crop.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> crop(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "256") Integer maxSize,
            @RequestParam(required = false) String geometry,
            @RequestParam(required = false) String location,
            //@RequestParam(required = false) String boundaries,
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
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
//        cropParameter.setBoundaries(boundaries);
        cropParameter.setMaxSize(maxSize);
        cropParameter.setComplete(complete);
        cropParameter.setZoom(zoom);
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
        cropParameter.setMaxBits(bits != null && bits.equals("max"));
        cropParameter.setBits(bits != null && !bits.equals("max") ? Integer.parseInt(bits) : null);
        cropParameter.setFormat(format);
        String etag = getRequestETag();
        return imageServerService.crop(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), cropParameter, etag, proxy);
    }

    @RequestMapping(value = "/imageinstance/{id}/window-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> window(
            @PathVariable Long id,
            @PathVariable String format,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer w,
            @PathVariable Integer h,
            @RequestParam(defaultValue = "false", required = false) Boolean withExterior,

            ProxyExchange<byte[]> proxy
    ) throws IOException, ParseException {
        log.debug("REST request get imageInstance {} window {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(withExterior);
        windowParameter.setFormat(format);

        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        String etag = getRequestETag();
        return imageServerService.window(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), windowParameter, etag, proxy);
    }

    @RequestMapping(value = "/imageinstance/{id}/camera-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> camera(
            @PathVariable Long id,
            @PathVariable String format,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer w,
            @PathVariable Integer h,

            ProxyExchange<byte[]> proxy
    ) throws IOException, ParseException {
        log.debug("REST request get imageInstance {} camera {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(false);
        windowParameter.setFormat(format);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        // TODO : should we handle other window parameters?
        String etag = getRequestETag();
        return imageServerService.window(sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage()), windowParameter, etag, proxy);
    }

    @GetMapping("/imageinstance/{id}/download")
    public ResponseEntity<byte[]> download(
        @PathVariable Long id,
        @RequestParam String Authorization,
        ProxyExchange<byte[]> proxy) throws IOException {
        log.debug("REST request to download image instance");
        ImageInstance imageinstance = imageInstanceService.find(id, Authorization)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        return imageServerService.download(imageinstance.getBaseImage(), proxy);
    }

    @GetMapping("/imageinstance/{id}/sliceinstance/reference.json")
    public ResponseEntity<String> getReferenceSlice(
            @PathVariable Long id
    ) {
        log.debug("REST request get reference sliceinstance for imageinstance {}", id);
        SliceInstance sliceInstance = imageInstanceService.getReferenceSlice(id);
        if (sliceInstance != null) {
            return responseSuccess(sliceInstance);
        } else {
            return responseNotFound("SliceInstance", "ImageInstance", id);
        }
    }

    @GetMapping("/imageinstance/{id}/metadata.json")
    public ResponseEntity<String> metadata(
            @PathVariable Long id
    ) throws IOException {
        log.debug("REST request get metadata for imageinstance {}", id);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        if (securityACLService.isFilterRequired(imageInstance.getProject())) {
            throw new ForbiddenException("You don't have the right to read or modify this resource! "  + imageInstance.getClass().toString() + " " + id);
        }
        return responseSuccess(imageServerService.rawProperties(imageInstance));
    }

    @GetMapping("/project/{projectId}/bounds/imageinstance.json")
    public ResponseEntity<String> bounds(
            @PathVariable Long projectId
    ) {
        log.debug("REST request get bouds for imageinstance in project {}", projectId);
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(JsonObject.toJsonString(imageInstanceService.computeBounds(project)));
    }


    protected void filterOneElement(Map<String, Object> element) {
        element.put("instanceFilename", null);
        element.put("filename", null);
        element.put("originalFilename", null);
        element.put("path", null);
        element.put("fullPath", null);
    }

    @GetMapping("/imagegroup/{imageGroup}/imageinstance.json")
    public ResponseEntity<String> listByImageGroup(@PathVariable Long imageGroup) {
        ImageGroup group = imageGroupService.get(imageGroup);
        if (group == null) {
            return responseNotFound("ImageInstance", "ImageGroup", imageGroup);
        }

        return responseSuccess(imageGroupImageInstanceService.list(group).stream().map(ImageGroupImageInstance::getImage).collect(Collectors.toList()));
    }
}
