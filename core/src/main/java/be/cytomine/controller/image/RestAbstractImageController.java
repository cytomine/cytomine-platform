package be.cytomine.controller.image;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.image.CropParameter;
import be.cytomine.dto.image.ImageParameter;
import be.cytomine.dto.image.LabelParameter;
import be.cytomine.dto.image.WindowParameter;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.AbstractImageService;
import be.cytomine.service.image.ImagePropertiesService;
import be.cytomine.service.image.SliceCoordinatesService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.utils.JsonObject;
import org.locationtech.jts.io.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestAbstractImageController extends RestCytomineController {

    private final AbstractImageService abstractImageService;

    private final ProjectService projectService;

    private final ImageServerService imageServerService;

    private final SliceCoordinatesService sliceCoordinatesService;

    private final ImagePropertiesService imagePropertiesService;

    @GetMapping("/abstractimage.json")
    public ResponseEntity<String> list(
            @RequestParam(value = "project", required = false) Long idProject
    ) {
        log.debug("REST request to list abstract image");
        Project project = idProject == null ? null : projectService.find(idProject).orElseThrow(() -> new ObjectNotFoundException("Project", idProject));
        return responseSuccess(abstractImageService.list(project, retrieveSearchParameters(), retrievePageable()));
    }

    @GetMapping("/abstractimage/{id}.json")
    public ResponseEntity<String> show(
           @PathVariable Long id
    ) {
        log.debug("REST request to get abstract image {}", id);
        return abstractImageService.find(id)
                .map(this::responseSuccess)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
    }

    @GetMapping("/uploadedfile/{id}/abstractimage.json")
    public ResponseEntity<String> getByUploadedFile(
            @PathVariable Long id
    ) {
        log.debug("REST request to get abstract image {}", id);
        return abstractImageService.findByUploadedFile(id)
                .map(this::responseSuccess)
                .orElseGet(() -> responseNotFound("AbstractImage", id));
    }


    @PostMapping("/abstractimage.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save abstractimage : " + json);
        return add(abstractImageService, json);
    }

    @PutMapping("/abstractimage/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit abstractimage : " + id);
        return update(abstractImageService, json);
    }

    @DeleteMapping("/abstractimage/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete abstractimage : " + id);
        return delete(abstractImageService, JsonObject.of("id", id), null);
    }

    @GetMapping("/abstractimage/unused.json")
    public ResponseEntity<String> listUnused() {
        log.debug("REST request to list unused abstractimages");
        return responseSuccess(abstractImageService.listUnused());
    }

    @GetMapping("/abstractimage/{id}/user.json")
    public ResponseEntity<String> showUploaderOfImage(@PathVariable Long id) {
        log.debug("REST request to show image uploader");
        User user = abstractImageService.getImageUploader(id);
        if (user !=null) {
            return responseSuccess(abstractImageService.getImageUploader(id));
        } else {
            return responseNotFound("AbstractImage", "User", id);
        }

    }

    // TODO:MIGRATION GET params vs POST params!
    @RequestMapping(value = "/abstractimage/{id}/thumb.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> thumb(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(required = false) Boolean refresh,
            @RequestParam(defaultValue = "512", required = false) Integer maxSize,
            @RequestParam(required = false) String colormap,
            @RequestParam(required = false) Boolean inverse,
            @RequestParam(required = false) Double contrast,
            @RequestParam(required = false) Double gamma,
            @RequestParam(required = false) String bits,
            @RequestParam(required = false) String Authorization,

            ProxyExchange<byte[]> proxy
    ) throws IOException {
        log.debug("REST request get abstractimage {} thumb {}", id, format);
        ImageParameter thumbParameter = new ImageParameter();
        thumbParameter.setFormat(format);
        thumbParameter.setMaxSize(maxSize);
        thumbParameter.setColormap(colormap);
        thumbParameter.setInverse(inverse);
        thumbParameter.setContrast(contrast);
        thumbParameter.setGamma(gamma);
        thumbParameter.setMaxBits(bits!=null && bits.equals("max"));
        thumbParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        thumbParameter.setRefresh(refresh);

        AbstractImage abstractImage = abstractImageService.find(id, Authorization)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        String etag = getRequestETag();
        return imageServerService.thumb(sliceCoordinatesService.getReferenceSlice(abstractImage), thumbParameter, etag, proxy);
    }


    @RequestMapping(value = "/abstractimage/{id}/preview.{format}", method = {RequestMethod.GET, RequestMethod.POST})
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
        log.debug("REST request get abstractimage {} preview {}", id, format);
        ImageParameter previewParameter = new ImageParameter();
        previewParameter.setFormat(format);
        previewParameter.setMaxSize(maxSize);
        previewParameter.setColormap(colormap);
        previewParameter.setInverse(inverse);
        previewParameter.setContrast(contrast);
        previewParameter.setGamma(gamma);
        previewParameter.setMaxBits(bits!=null &&  bits.equals("max"));
        previewParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);

        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        String etag = getRequestETag();
        return imageServerService.thumb(sliceCoordinatesService.getReferenceSlice(abstractImage), previewParameter, etag, proxy);
    }


    @GetMapping("/abstractimage/{id}/associated.json")
    public ResponseEntity<String> associated(@PathVariable Long id) throws IOException {
        log.debug("REST request to get available associated images");
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        return responseSuccess(imageServerService.associated(abstractImage));
    }

    @RequestMapping(value = "/abstractimage/{id}/associated/{label}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> label(
            @PathVariable Long id,
            @PathVariable String label,
            @PathVariable String format,
            @RequestParam(defaultValue = "256") Integer maxSize,

            ProxyExchange<byte[]> proxy) throws IOException {
        log.debug("REST request to get associated image of a abstract image");
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        LabelParameter labelParameter = new LabelParameter();
        labelParameter.setFormat(format);
        labelParameter.setLabel(label);
        labelParameter.setMaxSize(maxSize);
        String etag = getRequestETag();
        return imageServerService.label(abstractImage, labelParameter, etag, proxy);
    }

    @RequestMapping(value = "/abstractimage/{id}/crop.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> crop(
            @PathVariable Long id,
            @PathVariable String format,
            @RequestParam(defaultValue = "256") Integer maxSize,
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
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
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
        cropParameter.setMaxBits(bits!=null && bits.equals("max"));
        cropParameter.setBits(bits!=null && !bits.equals("max") ? Integer.parseInt(bits): null);
        cropParameter.setFormat(format);

        String etag = getRequestETag();
        return imageServerService.crop(sliceCoordinatesService.getReferenceSlice(abstractImage), cropParameter, etag, proxy);
    }

    @RequestMapping(value = "/abstractimage/{id}/window-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
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
        log.debug("REST request get abstractimage {} window {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(withExterior);
        windowParameter.setFormat(format);
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        String etag = getRequestETag();
        return imageServerService.window(sliceCoordinatesService.getReferenceSlice(abstractImage), windowParameter, etag, proxy);
    }

    @RequestMapping(value = "/abstractimage/{id}/camera-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> camera(
            @PathVariable Long id,
            @PathVariable String format,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer w,
            @PathVariable Integer h,

            ProxyExchange<byte[]> proxy
    ) throws IOException, ParseException {
        log.debug("REST request get abstractimage {} camera {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(false);
        windowParameter.setFormat(format);
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        String etag = getRequestETag();
        return imageServerService.window(sliceCoordinatesService.getReferenceSlice(abstractImage), windowParameter, etag, proxy);
    }

    @GetMapping("/abstractimage/{id}/metadata.json")
    public ResponseEntity<String> metadata(
            @PathVariable Long id
    ) throws IOException {
        log.debug("REST request get metadata for abstractimage {}", id);
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        return responseSuccess(imageServerService.rawProperties(abstractImage));
    }

    @PostMapping("/abstractimage/{id}/properties/extract.json")
    public ResponseEntity<String> extractUseful(@PathVariable Long id) throws IOException, IllegalAccessException {
        log.debug("REST request to get available associated images");
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        imagePropertiesService.extractUseful(abstractImage);
        return responseSuccess(new JsonObject());
    }

    @PostMapping("/abstractimage/{id}/properties/regenerate.json")
    public ResponseEntity<String> regenerateProperties(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") Boolean deep
    ) throws IOException, IllegalAccessException {
        log.debug("REST request to get available associated images");
        AbstractImage abstractImage = abstractImageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("AbstractImage", id));
        imagePropertiesService.regenerate(abstractImage, deep);
        return responseSuccess(new JsonObject());
    }

    @GetMapping("/abstractimage/{id}/download")
    public ResponseEntity<byte[]> download(
        @PathVariable Long id,
        @RequestParam String Authorization,
        ProxyExchange<byte[]> proxy) throws IOException {
        log.debug("REST request to download image instance");
        AbstractImage abstractImage = abstractImageService.find(id, Authorization)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return imageServerService.download(abstractImage, proxy);
    }
}
