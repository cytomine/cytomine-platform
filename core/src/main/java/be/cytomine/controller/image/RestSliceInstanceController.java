package be.cytomine.controller.image;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.io.ParseException;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.dto.image.CropParameter;
import be.cytomine.dto.image.ImageParameter;
import be.cytomine.dto.image.TileParameters;
import be.cytomine.dto.image.WindowParameter;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.image.*;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.utils.JsonObject;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestSliceInstanceController extends RestCytomineController {

    private final SliceInstanceService sliceInstanceService;

    private final ImageInstanceService imageInstanceService;

    private final ImageServerService imageServerService;

    @GetMapping("/imageinstance/{id}/sliceinstance.json")
    public ResponseEntity<String> listByImageInstance(
            @PathVariable Long id
    ) {
        log.debug("REST request to list slice instance for image {}", id);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return responseSuccess(sliceInstanceService.list(imageInstance));
    }

    @GetMapping("/sliceinstance/{id}.json")
    public ResponseEntity<String> show(
            @PathVariable Long id
    ) {
        log.debug("REST request to get slice instance {}", id);
        return sliceInstanceService.find(id)
                .map(this::responseSuccess)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id));
    }

    @GetMapping("/imageinstance/{id}/{channel}/{zStack}/{time}/sliceinstance.json")
    public ResponseEntity<String> getByImageInstanceAndCoordinates(
            @PathVariable Long id,
            @PathVariable Integer channel,
            @PathVariable Integer zStack,
            @PathVariable Integer time
    ) {
        log.debug("REST request to get slice instance for  image {} and coordinates {}-{}-{}", id, channel, zStack, time);
        ImageInstance imageInstance = imageInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));

        SliceInstance sliceInstance = sliceInstanceService.find(imageInstance, channel,zStack, time)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id + "[" + channel + "-" + zStack + "-" + time + "]"));
        return responseSuccess(sliceInstance);
    }
    
    @PostMapping("/sliceinstance.json")
    public ResponseEntity<String> add(@RequestBody String json) {
        log.debug("REST request to save sliceinstance : " + json);
        return add(sliceInstanceService, json);
    }

    @PutMapping("/sliceinstance/{id}.json")
    public ResponseEntity<String> edit(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to edit sliceinstance : " + id);
        return update(sliceInstanceService, json);
    }

    @DeleteMapping("/sliceinstance/{id}.json")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.debug("REST request to delete sliceinstance : " + id);
        return delete(sliceInstanceService, JsonObject.of("id", id), null);
    }

    @GetMapping("/sliceinstance/{id}/normalized-tile/zoom/{z}/tx/{tx}/ty/{ty}.{format}")
    public ResponseEntity<byte[]> tile(
            @PathVariable Long id,
            @PathVariable Long z,
            @PathVariable Long tx,
            @PathVariable Long ty,

            @PathVariable String format,
            @RequestParam(required = false) String channels,
            @RequestParam(required = false) String zSlices,
            @RequestParam(required = false) String timepoints,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false) String minIntensities,
            @RequestParam(required = false) String maxIntensities,
            @RequestParam(required = false) String gammas,
            @RequestParam(required = false) String colormaps,

            ProxyExchange<byte[]> proxy
    ) throws IOException {
        /* Request parameter validation is delegated to PIMS to avoid double validation. Moreover, these parameter
        validation is complex as they can accept multiple types: e.g. 'gammas' accept a Double or List<Double> whose
        length is defined by the number of selected channels in 'channels' parameter.
         */

        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id));

        TileParameters tileParameters = new TileParameters();
        tileParameters.setFormat(format);
        tileParameters.setZoom(z);
        tileParameters.setTx(tx);
        tileParameters.setTy(ty);
        tileParameters.setChannels(channels);
        tileParameters.setZSlices(zSlices);
        tileParameters.setTimepoints(timepoints);
        tileParameters.setFilters(filters);
        tileParameters.setMinIntensities(minIntensities);
        tileParameters.setMaxIntensities(maxIntensities);
        tileParameters.setGammas(gammas);
        tileParameters.setColormaps(colormaps);

        String etag = getRequestETag();
        return imageServerService.normalizedTile(sliceInstance, tileParameters, etag, proxy);
    }

//    // TODO:MIGRATION GET params vs POST params!
    @RequestMapping(value = "/sliceinstance/{id}/thumb.{format}", method = {RequestMethod.GET, RequestMethod.POST})
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

            ProxyExchange<byte[]> proxy
    ) throws IOException {
        log.debug("REST request get sliceinstance {} thumb {}", id, format);
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

        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id));

        String etag = getRequestETag();
        return imageServerService.thumb(sliceInstance, thumbParameter, etag, proxy);
    }

    @RequestMapping(value = "/sliceinstance/{id}/crop.{format}", method = {RequestMethod.GET, RequestMethod.POST})
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
        log.debug("REST request to get associated image of a slice instance");
        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id));

        CropParameter cropParameter = new CropParameter();
        cropParameter.setGeometry(geometry);
        cropParameter.setLocation(location);
//        cropParameter.setBoundaries(boundaries);
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

        return imageServerService.crop(sliceInstance.getBaseSlice(), cropParameter,etag, proxy);
    }

    @RequestMapping(value = "/sliceinstance/{id}/window-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
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
        log.debug("REST request get sliceinstance {} window {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(withExterior);
        windowParameter.setFormat(format);
        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id));

        //TODO:
//        if (params.mask || params.alphaMask || params.alphaMask || params.draw || params.type in ['draw', 'mask', 'alphaMask', 'alphamask'])
//            params.location = getWKTGeometry(sliceInstance, params)

        String etag = getRequestETag();
        return  imageServerService.window(sliceInstance.getBaseSlice(), windowParameter, etag, proxy);
    }

    @RequestMapping(value = "/sliceinstance/{id}/camera-{x}-{y}-{w}-{h}.{format}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> camera(
            @PathVariable Long id,
            @PathVariable String format,
            @PathVariable Integer x,
            @PathVariable Integer y,
            @PathVariable Integer w,
            @PathVariable Integer h,

            ProxyExchange<byte[]> proxy
    ) throws IOException, ParseException {
        log.debug("REST request get sliceinstance {} camera {}", id, format);
        WindowParameter windowParameter = new WindowParameter();
        windowParameter.setX(x);
        windowParameter.setY(y);
        windowParameter.setW(w);
        windowParameter.setH(h);
        windowParameter.setWithExterior(false);
        windowParameter.setFormat(format);
        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id));

        String etag = getRequestETag();
        return  imageServerService.window(sliceInstance.getBaseSlice(), windowParameter, etag, proxy);
    }

    @GetMapping("/sliceinstance/{id}/histogram.json")
    public ResponseEntity<String> histogram(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "256") Integer nBins) throws IOException {
        log.debug("REST request to get histogram slice");
        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("sliceInstance", id));
        return responseSuccess(imageServerService.planeHistograms(sliceInstance.getBaseSlice(), nBins, false));
    }

    @GetMapping("/sliceinstance/{id}/histogram/bounds.json")
    public ResponseEntity<String> histogramBounds(@PathVariable Long id) throws IOException {
        log.debug("REST request to get historigramBounds slice");
        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("sliceInstance", id));
        return responseSuccess(imageServerService.planeHistogramBounds(sliceInstance.getBaseSlice(), false));
    }

    @GetMapping("/sliceinstance/{id}/channelhistogram.json")
    public ResponseEntity<String> channelHistograms(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "256") Integer nBins) throws IOException {
        log.debug("REST request to get channelhistogram slice");
        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("sliceInstance", id));
        return responseSuccess(imageServerService.planeHistograms(sliceInstance.getBaseSlice(), nBins, true));
    }

    @GetMapping("/sliceinstance/{id}/channelhistogram/bounds.json")
    public ResponseEntity<String> channelHistogramBounds(@PathVariable Long id) throws IOException {
        log.debug("REST request to get channelHistogramBounds slice");
        SliceInstance sliceInstance = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("sliceInstance", id));
        return responseSuccess(imageServerService.planeHistogramBounds(sliceInstance.getBaseSlice(), true));
    }




    //TODO

//    //todo : move into a service
//    def userAnnotationService
//    def reviewedAnnotationService
//    def termService
//    def userService
//    def annotationListingService
//    public String getWKTGeometry(SliceInstance sliceInstance, params) {
//        def geometries = []
//        if (params.annotations && !params.reviewed) {
//            def idAnnotations = params.annotations.split(',')
//            idAnnotations.each { idAnnotation ->
//                    def annot = userAnnotationService.read(idAnnotation)
//                if (annot)
//                    geometries << annot.location
//            }
//        }
//        else if (params.annotations && params.reviewed) {
//            def idAnnotations = params.annotations.split(',')
//            idAnnotations.each { idAnnotation ->
//                    def annot = reviewedAnnotationService.read(idAnnotation)
//                if (annot)
//                    geometries << annot.location
//            }
//        }
//        else if (!params.annotations) {
//            def project = sliceInstance.image.project
//            List<Long> termsIDS = params.terms?.split(',')?.collect {
//                Long.parseLong(it)
//            }
//            if (!termsIDS) { //don't filter by term, take everything
//                termsIDS = termService.getAllTermId(project)
//            }
//
//            List<Long> userIDS = params.users?.split(",")?.collect {
//                Long.parseLong(it)
//            }
//            if (!userIDS) { //don't filter by users, take everything
//                userIDS = userService.listLayers(project).collect { it.id}
//            }
//            List<Long> sliceIDS = [sliceInstance.id]
//
//            log.info params
//            //Create a geometry corresponding to the ROI of the request (x,y,w,h)
//            int x
//            int y
//            int w
//            int h
//            try {
//                x = params.int('topLeftX')
//                y = params.int('topLeftY')
//                w = params.int('width')
//                h = params.int('height')
//            }catch (Exception e) {
//                x = params.int('x')
//                y = params.int('y')
//                w = params.int('w')
//                h = params.int('h')
//            }
//            Geometry roiGeometry = GeometryUtils.createBoundingBox(
//                    x,                                      //minX
//                    x + w,                                  //maxX
//                    sliceInstance.baseSlice.image.height - (y + h),    //minX
//                    sliceInstance.baseSlice.image.height - y           //maxY
//            )
//
//
//            //Fetch annotations with the requested term on the request image
//            if (params.review) {
//                ReviewedAnnotationListing ral = new ReviewedAnnotationListing(
//                        project: project.id, terms: termsIDS, reviewUsers: userIDS, slices:sliceIDS, bbox:roiGeometry,
//                        columnToPrint:['basic', 'meta', 'wkt', 'term']
//                )
//                def result = annotationListingService.listGeneric(ral)
//                log.info "annotations=${result.size()}"
//                geometries = result.collect {
//                    new WKTReader().read(it["location"])
//                }
//
//            } else {
//                log.info "roiGeometry=${roiGeometry}"
//                log.info "termsIDS=${termsIDS}"
//                log.info "userIDS=${userIDS}"
//                Collection<UserAnnotation> annotations = userAnnotationService.list(sliceInstance, roiGeometry, termsIDS, userIDS)
//                log.info "annotations=${annotations.size()}"
//                geometries = annotations.collect { geometry ->
//                        geometry.getLocation()
//                }
//            }
//        }
//        GeometryCollection geometryCollection = new GeometryCollection((Geometry[])geometries, new GeometryFactory())
//        return new WKTWriter().write(geometryCollection)
//    }

}
