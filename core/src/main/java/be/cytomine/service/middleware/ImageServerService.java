package be.cytomine.service.middleware;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.image.*;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.dto.StorageStats;
import be.cytomine.dto.image.CropParameter;
import be.cytomine.dto.image.ImageParameter;
import be.cytomine.dto.image.LabelParameter;
import be.cytomine.dto.image.TileParameters;
import be.cytomine.dto.image.WindowParameter;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.utils.SimplifyGeometryService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.PreparedRequest;
import be.cytomine.utils.StringUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Slf4j
@Service
@Transactional
public class ImageServerService {
    // Internal communication to image server must use this base path as a convention.
    public static final String IMS_API_BASE_PATH = "/ims";

    @Value("${application.internalProxyURL}")
    String internalProxyURL;

    @Autowired
    private ImageInstanceService imageInstanceService;

    @Autowired
    private SimplifyGeometryService simplifyGeometryService;

    @Autowired
    public void setImageInstanceService(ImageInstanceService imageInstanceService) {
        this.imageInstanceService = imageInstanceService;
    }

    public String internalImageServerURL() {
        return this.internalProxyURL + IMS_API_BASE_PATH;
    }

    public StorageStats storageSpace() throws IOException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("storage/size.json");

        return JsonObject.toObject(request.toObject(String.class), StorageStats.class);
    }

    public List<Map<String, Object>> formats() {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("formats");

        JsonObject jsonObject = JsonObject.toJsonObject(request.toObject(String.class));
        return ((List<Map<String,Object>>)jsonObject.get("items")).stream().map(StringUtils::keysToCamelCase).toList();
    }

    public ResponseEntity<byte[]> download(UploadedFile uploadedFile, ProxyExchange<byte[]> proxy) throws IOException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("file");
        request.addPathFragment(uploadedFile.getPath(), true);
        request.addPathFragment("export");
        request.addQueryParameter("filename", uploadedFile.getOriginalFilename());
        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return request.toResponseEntity(proxy, byte[].class);
    }

    public ResponseEntity<byte[]> download(AbstractImage abstractImage, ProxyExchange<byte[]> proxy) throws IOException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(abstractImage.getPath(), true);
        request.addPathFragment("export");
        request.addQueryParameter("filename", abstractImage.getOriginalFilename());
        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return request.toResponseEntity(proxy, byte[].class);
    }

    public ResponseEntity<byte[]> download(CompanionFile companionFile, ProxyExchange<byte[]> proxy) throws IOException {
        return download(companionFile.getUploadedFile(), proxy);
    }

    public Map<String, Object> properties(AbstractImage image) throws IOException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(image.getPath(), true);
        request.addPathFragment("info");

        return JsonObject.toMap(request.toObject(String.class));
    }

    public List<Map<String, Object>> rawProperties(AbstractImage image) throws IOException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(image.getPath(), true);
        request.addPathFragment("metadata");

        return JsonObject.toJsonObject(request.toObject(String.class))
                .getJSONAttrListMap("items").stream().map(StringUtils::keysToCamelCase).toList();
    }

    public List<Map<String, Object>> rawProperties(ImageInstance image) throws IOException {
        return this.rawProperties(image.getBaseImage());
    }

    public Map<String, Object> imageHistogram(AbstractImage image, int nBins) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(image.getPath(), true);
        request.addPathFragment("histogram");
        request.addPathFragment("per-image");
        request.addQueryParameter("n_bins", nBins);

        Map<String, Object> json = JsonObject.toMap(request.toObject(String.class));
        return StringUtils.keysToCamelCase(json);
    }

    public Map<String, Object> imageHistogramBounds(AbstractImage image) {
        return imageHistogramBounds(image, 256);
    }

    public Map<String, Object> imageHistogramBounds(AbstractImage image, int nBins) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(image.getPath(), true);
        request.addPathFragment("histogram");
        request.addPathFragment("per-image");
        request.addPathFragment("bounds");
        request.addQueryParameter("n_bins", nBins);

        Map<String, Object> json = JsonObject.toMap(request.toObject(String.class));
        return StringUtils.keysToCamelCase(json);
    }

    public List<Map<String, Object>> channelHistograms(AbstractImage image, int nBins) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(image.getPath(), true);
        request.addPathFragment("histogram");
        request.addPathFragment("per-channels");
        request.addQueryParameter("n_bins", nBins);

        Map<String, Object> json = JsonObject.toMap(request.toObject(String.class));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }


    public List<Map<String, Object>> channelHistogramBounds(AbstractImage image) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(image.getPath(), true);
        request.addPathFragment("histogram");
        request.addPathFragment("per-channels");
        request.addPathFragment("bounds");

        Map<String, Object> json = JsonObject.toMap(request.toObject(String.class));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }


    public List<Map<String, Object>> planeHistograms(AbstractSlice slice, int nBins, boolean allChannels) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(slice.getPath(), true);
        request.addPathFragment("histogram");
        request.addPathFragment("per-plane");
        request.addPathFragment("z");
        request.addPathFragment(slice.getZStack().toString());
        request.addPathFragment("t");
        request.addPathFragment(slice.getTime().toString());
        request.addQueryParameter("n_bins", nBins);
        if (!allChannels) {
            request.addQueryParameter("channels", slice.getChannel());
        }

        Map<String, Object> json = JsonObject.toMap(request.toObject(String.class));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }


    public List<Map<String, Object>> planeHistogramBounds(AbstractSlice slice, boolean allChannels) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(slice.getPath(), true);
        request.addPathFragment("histogram");
        request.addPathFragment("per-plane");
        request.addPathFragment("z");
        request.addPathFragment(slice.getZStack().toString());
        request.addPathFragment("t");
        request.addPathFragment(slice.getTime().toString());
        request.addPathFragment("bounds");
        if (!allChannels) {
            request.addQueryParameter("channels", slice.getChannel());
        }

        Map<String, Object> json = JsonObject.toMap(request.toObject(String.class));
        List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
        return items.stream().map(x -> renameChannelHistogramKeys(StringUtils.keysToCamelCase(x))).toList();
    }

    public List<String> associated(AbstractImage image) throws IOException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(image.getPath(), true);
        request.addPathFragment("info");
        request.addPathFragment("associated");

        return JsonObject.toJsonObject(request.toObject(String.class))
                .getJSONAttrListMap("items").stream().map(x -> (String)x.get("name")).toList();
    }

    public List<String> associated(ImageInstance image) throws IOException {
        return associated(image.getBaseImage());
    }

    public ResponseEntity<byte[]> label(ImageInstance image, LabelParameter params, String etag, ProxyExchange<byte[]> proxy) {
        return label(image.getBaseImage(), params, etag, proxy);
    }

    public ResponseEntity<byte[]> label(AbstractImage image, LabelParameter params, String etag, ProxyExchange<byte[]> proxy) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(image.getPath(), true);
        request.addPathFragment("associated");
        request.addPathFragment(params.getLabel().toLowerCase());
        request.addQueryParameter("length", params.getMaxSize());

        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        request.getHeaders().add(org.springframework.http.HttpHeaders.ACCEPT, formatToMediaType(params.getFormat()));
        if (etag != null) {
            request.getHeaders().add(org.springframework.http.HttpHeaders.IF_NONE_MATCH, etag);
        }

        return request.toResponseEntity(proxy, byte[].class);
    }

    public ResponseEntity<byte[]> thumb(ImageInstance image, ImageParameter params, String etag, ProxyExchange<byte[]> proxy)  {
        return thumb(imageInstanceService.getReferenceSlice(image), params, etag, proxy);
    }

    public ResponseEntity<byte[]> thumb(SliceInstance slice, ImageParameter params, String etag, ProxyExchange<byte[]> proxy)  {
        return thumb(slice.getBaseSlice(), params, etag, proxy);
    }

    public ResponseEntity<byte[]> thumb(AbstractSlice slice, ImageParameter params, String etag, ProxyExchange<byte[]> proxy) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(slice.getPath(), true);
        request.addPathFragment((params.getBits() != null) ? "resized" : "thumb");

        if (slice.getImage().getChannels()!=null && slice.getImage().getChannels() > 1) {
            request.addQueryParameter("channels", slice.getChannel());
            // Ensure that if the slice is RGB, the 3 intrinsic channels are used
        }
        request.addQueryParameter("z_slices", slice.getZStack());
        request.addQueryParameter("timepoints", slice.getTime());

        request.addQueryParameter("length", params.getMaxSize());
        request.addQueryParameter("gammas", params.getGamma());
        if (params.getColormap() != null) {
            request.addQueryParameter("colormaps", Arrays.stream(params.getColormap().split(",")).toList());
        }
        if (params.getBits() != null) {
            request.addQueryParameter("bits", params.getMaxBits() ? "AUTO" : params.getBits());
        }
        if (params.getInverse() != null && params.getInverse()) {
            if (params.getColormap() != null) {
                request.addQueryParameter("colormaps", Arrays.stream(params.getColormap().split(","))
                        .map(ImageServerService::invertColormap)
                        .toList()
                );
            }
            else {
                request.addQueryParameter("colormaps", "!DEFAULT");
            }
        }
        
        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        request.getHeaders().add(org.springframework.http.HttpHeaders.ACCEPT, formatToMediaType(params.getFormat()));
        if (etag != null) {
            request.getHeaders().add(org.springframework.http.HttpHeaders.IF_NONE_MATCH, etag);
        }
        return request.toResponseEntity(proxy, byte[].class);
    }

    public ResponseEntity<byte[]> normalizedTile(SliceInstance slice, TileParameters params, String etag, ProxyExchange<byte[]> proxy)  {
        return normalizedTile(slice.getBaseSlice(), params, etag, proxy);
    }

    public ResponseEntity<byte[]> normalizedTile(AbstractSlice slice, TileParameters params, String etag, ProxyExchange<byte[]> proxy) {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.GET);
        request.setUrl(this.internalImageServerURL());

        request.addPathFragment("image");
        request.addPathFragment(slice.getPath(), true);
        request.addPathFragment("normalized-tile");
        request.addPathFragment("zoom");
        request.addPathFragment(params.getZoom().toString());
        request.addPathFragment("tx");
        request.addPathFragment(params.getTx().toString());
        request.addPathFragment("ty");
        request.addPathFragment(params.getTy().toString());

        if (params.getChannels() != null) {
            request.addQueryParameter("channels", params.getChannels());
        }
        else if (slice.getImage().getChannels() != null && slice.getImage().getChannels() > 1) {
            request.addQueryParameter("channels", slice.getChannel());
            // Ensure that if the slice is RGB, the 3 intrinsic channels are used
        }

        request.addQueryParameter("z_slices", params.getZSlices() != null ? params.getZSlices() : slice.getZStack());
        request.addQueryParameter("timepoints", params.getTimepoints() != null ? params.getTimepoints() : slice.getTime());

        request.addQueryParameter("gammas", params.getGammas());
        request.addQueryParameter("colormaps", params.getColormaps());
        request.addQueryParameter("min_intensities", params.getMinIntensities());
        request.addQueryParameter("max_intensities", params.getMaxIntensities());
        request.addQueryParameter("filters", params.getFilters());

        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        request.getHeaders().add(org.springframework.http.HttpHeaders.ACCEPT, formatToMediaType(params.getFormat()));
        if (etag != null) {
            request.getHeaders().add(org.springframework.http.HttpHeaders.IF_NONE_MATCH, etag);
        }

        return request.toResponseEntity(proxy, byte[].class);
    }


    public ResponseEntity<byte[]> crop(AnnotationDomain annotation, CropParameter params, String etag, ProxyExchange<byte[]> proxy) throws UnsupportedEncodingException, ParseException {
        params.setLocation(annotation.getWktLocation());
        return crop(annotation.getSlice().getBaseSlice(), params, etag, proxy);
    }

    public ResponseEntity<byte[]> crop(SliceInstance slice, CropParameter params, String etag, ProxyExchange<byte[]> proxy) throws UnsupportedEncodingException, ParseException {
        return crop(slice.getBaseSlice(), params, etag, proxy);
    }

    public ResponseEntity<byte[]> crop(AbstractSlice slice, CropParameter params, String etag, ProxyExchange<byte[]> proxy) throws UnsupportedEncodingException, ParseException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.POST);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(slice.getPath(), true);
        request.addPathFragment(getCropUri(params));

        JsonObject body = new JsonObject();

        // CZT
        if (slice.getImage().getChannels()!=null && slice.getImage().getChannels() > 1) {
            body.put("channels", slice.getChannel());
            // Ensure that if the slice is RGB, the 3 intrinsic channels are used
        }
        body.put("z_slices", slice.getZStack());
        body.put("timepoints", slice.getTime());

        // Image processing
        body.put("context_factor", params.getIncreaseArea());
        body.put("length", params.getMaxSize());
        body.put("gammas", params.getGamma());
        if (params.getColormap() != null) {
            body.put("colormaps", Arrays.stream(params.getColormap().split(",")).toList());
        }
        if (params.getBits() != null) {
            body.put("bits", params.getMaxBits() ? "AUTO" : params.getBits());
        }
        if (params.getInverse() != null && params.getInverse()) {
            if (params.getColormap() != null) {
                body.put("colormaps", Arrays.stream(params.getColormap().split(","))
                        .map(ImageServerService::invertColormap)
                        .toList()
                );
            }
            else {
                body.put("colormaps", "!DEFAULT");
            }
        }
        if (params.getMaxSize() == null) {
            body.put("level", params.getZoom() != null ? params.getZoom() : 0);
        }
        if (params.getBits() != null) {
            body.put("bits", params.getMaxBits() ? "AUTO" : params.getBits());
        }

        // Annotations
        Object geometry = params.getGeometry();
        if (geometry!=null && geometry instanceof String) {
            geometry = new WKTReader().read((String)geometry);
        }

        if (StringUtils.isBlank(params.getGeometry()) && StringUtils.isNotBlank(params.getLocation())) {
            geometry = new WKTReader().read(params.getLocation());
        }
        String wkt = null;
        if (params.getComplete()!=null && params.getComplete() && geometry!=null) {
            wkt = simplifyGeometryService.reduceGeometryPrecision((Geometry)geometry).toText();
        } else if (geometry!=null) {
            wkt = simplifyGeometryService.simplifyPolygonForCrop((Geometry)geometry).toText();
        }

        ArrayList<Map<String, Object>> annotations = new ArrayList<>();
        if (wkt != null) {
            LinkedHashMap<String, Object> annot = new LinkedHashMap<>();
            annot.put("geometry", wkt);
            annotations.add(annot);
        }

        if (params.getDraw() != null && params.getDraw()) {
            body.put("try_square", params.getSquare());
            String color = params.getColor() != null ? params.getColor().replace("0x", "#") : null;
            annotations.forEach(annot -> {
                annot.put("stroke_color", color);
                annot.put("stroke_width", params.getThickness());
            });
        } else if (params.getMask() != null && params.getMask()) {
            annotations.forEach(annot -> {
                annot.put("fill_color", "#fff");
            });
        } else if (params.getAlphaMask() != null && params.getAlphaMask()) {
            body.put("background_transparency", params.getAlpha() != null ? params.getAlpha() : 100);
        } else {
            body.put("background_transparency", 0);
        }
        body.put("annotations", annotations);
        request.setJsonBody(body);

        request.getHeaders().add("X-Annotation-Origin", "LEFT_BOTTOM");
        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        String format = retrieveCropFormat(params);
        request.getHeaders().add(org.springframework.http.HttpHeaders.ACCEPT, formatToMediaType(format));
        if (etag != null) {
            request.getHeaders().add(org.springframework.http.HttpHeaders.IF_NONE_MATCH, etag);
        }
        if (params.getSafe() != null && params.getSafe()) {
            request.getHeaders().add("X-Image-Size-Safety", "SAFE_RESIZE");
        }

        return request.toResponseEntity(proxy, byte[].class);
    }

    private static String retrieveCropFormat(CropParameter cropParameter) {
        String format;
        if (cropParameter.getDraw()!=null && cropParameter.getDraw()) {
            format = checkFormat(cropParameter.getFormat(), List.of("jpg", "png", "webp"));
        } else if (cropParameter.getMask()!=null && cropParameter.getMask()) {
            format = checkFormat(cropParameter.getFormat(), List.of("jpg", "png", "webp"));
        } else if (cropParameter.getAlphaMask()!=null && cropParameter.getAlphaMask()) {
            format = checkFormat(cropParameter.getFormat(), List.of("png", "webp"));
        } else {
            format = checkFormat(cropParameter.getFormat(), List.of("jpg", "png", "webp"));
        }
        return format;
    }

    private static String getCropUri(CropParameter cropParameter)  {
        if (cropParameter.getDraw()!=null && cropParameter.getDraw()) {
            return "/annotation/drawing";
        } else if (cropParameter.getMask()!=null && cropParameter.getMask()) {
            return "/annotation/mask";
        } else if (cropParameter.getAlphaMask()!=null && cropParameter.getAlphaMask()) {
            return  "/annotation/crop";
        } else {
            return "/annotation/crop";
        }
    }


    public ResponseEntity<byte[]> window(AbstractSlice slice, WindowParameter params, String etag, ProxyExchange<byte[]> proxy) throws UnsupportedEncodingException, ParseException {
        PreparedRequest request = new PreparedRequest();
        request.setMethod(HttpMethod.POST);
        request.setUrl(this.internalImageServerURL());
        request.addPathFragment("image");
        request.addPathFragment(slice.getPath(), true);
        request.addPathFragment("window");

        JsonObject body = new JsonObject();

        LinkedHashMap<String, Object> region = new LinkedHashMap<>();
        region.put("left", params.getX());
        region.put("top", params.getY());
        region.put("width", params.getW());
        region.put("height", params.getH());
        body.put("region", region);

        // CZT
        if (slice.getImage().getChannels()!=null && slice.getImage().getChannels() > 1) {
            body.put("channels", slice.getChannel());
            // Ensure that if the slice is RGB, the 3 intrinsic channels are used
        }
        body.put("z_slices", slice.getZStack());
        body.put("timepoints", slice.getTime());

        // Image processing
        body.put("length", params.getMaxSize());
        body.put("gammas", params.getGamma());
        if (params.getColormap() != null) {
            body.put("colormaps", Arrays.stream(params.getColormap().split(",")).toList());
        }
        if (params.getBits() != null) {
            body.put("bits", params.getMaxBits() ? "AUTO" : params.getBits());
        }
        if (params.getInverse() != null && params.getInverse()) {
            if (params.getColormap() != null) {
                body.put("colormaps", Arrays.stream(params.getColormap().split(","))
                        .map(ImageServerService::invertColormap)
                        .toList()
                );
            }
            else {
                body.put("colormaps", "!DEFAULT");
            }
        }
        if (params.getMaxSize() == null) {
            body.put("level", params.getZoom() != null ? params.getZoom() : 0);
        }
        if (params.getBits() != null) {
            body.put("bits", params.getMaxBits() ? "AUTO" : params.getBits());
        }

        if (params.getGeometries() != null) {
            String strokeColor = params.getColor() != null ? params.getColor().replace("0x", "#") : "black";
            Integer strokeWidth = params.getThickness() != null ? params.getThickness() : 1;
            String annotationType = checkType(params);


            List<Map<String, Object>> geometries =  params.getGeometries();
            List<Map<String, Object>> annotations = new ArrayList<>();
            for (Map<String, Object> geometry : geometries) {
                String wkt = null;
                if (params.getComplete()!=null && params.getComplete() && geometry!=null) {
                    wkt = simplifyGeometryService.reduceGeometryPrecision((Geometry)geometry).toText();
                } else if (geometry!=null) {
                    wkt = simplifyGeometryService.simplifyPolygonForCrop((Geometry)geometry).toText();
                }
                Map<String, Object> annot = new LinkedHashMap<>(wkt!=null? Map.of("geometry", wkt) : Map.of());

                if (annotationType.equals("draw")) {
                    annot.put("stroke_color", strokeColor);
                    annot.put("stroke_width", strokeWidth);
                } else if (annotationType.equals("mask")) {
                    annot.put("fill_color", "#fff");
                }
                annotations.add(annot);
            }

            Map<String, Object> annotationStyle = new HashMap<>();
            switch (annotationType) {
                case "draw" -> annotationStyle.put("mode", "DRAWING");
                case "mask" -> annotationStyle.put("mode", "MASK");
                case "alphaMask", "alphamask" -> {
                    annotationStyle.put("mode", "CROP");
                    annotationStyle.put("background_transparency", params.getAlpha() != null ? params.getAlpha() : 100);
                }
                default -> {
                    annotationStyle.put("mode", "CROP");
                    annotationStyle.put("background_transparency", 0);
                }
            }
            body.put("annotation_style", annotationStyle);
            body.put("annotations", annotations);
        }

        request.setJsonBody(body);

        request.getHeaders().add("X-Annotation-Origin", "LEFT_BOTTOM");
        request.getHeaders().add(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        String format = retrieveWindowFormat(params);
        request.getHeaders().add(org.springframework.http.HttpHeaders.ACCEPT, formatToMediaType(format));
        if (etag != null) {
            request.getHeaders().add(org.springframework.http.HttpHeaders.IF_NONE_MATCH, etag);
        }
        if (params.getSafe() != null && params.getSafe()) {
            request.getHeaders().add("X-Image-Size-Safety", "SAFE_RESIZE");
        }

        return request.toResponseEntity(proxy, byte[].class);
    }

    private static String retrieveWindowFormat(WindowParameter params) {
        String format;
        if (checkType(params).equals("alphamask")) {
            format = checkFormat(params.getFormat(), List.of("png", "webp"));
        } else {
            format = checkFormat(params.getFormat(), List.of("png", "jpg", "webp"));
        }
        return format;
    }

    private static String checkType(WindowParameter params) {
        if ((params.getDraw()!=null && params.getDraw()) || (params.getType()!=null && params.getType().equals("draw")))
            return "draw";
        else if ((params.getMask()!=null && params.getMask()) || (params.getType()!=null && params.getType().equals("mask")))
            return "mask";
        else if ((params.getAlphaMask()!=null && params.getAlphaMask()) || (params.getType()!=null && (params.getType().equals("alphaMask") || params.getType().equals("alphamask")))) {
            return "alphaMask";
        } else {
            return "crop";
        }
    }
    
    private static String formatToMediaType(String format) {
        return formatToMediaType(format, MediaType.IMAGE_JPEG_VALUE);
    }

    private static String formatToMediaType(String format, String defaultMediaType) {
        return switch (format) {
            case "json" -> MediaType.APPLICATION_JSON_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "webp" -> "image/webp";
            case "jpg" -> MediaType.IMAGE_JPEG_VALUE;
            default -> defaultMediaType;
        };
    }

    private static String checkFormat(String format, List<String> accepted) {
        if (accepted==null) {
            accepted = List.of("jpg");
        }
        if (format==null) {
            throw new WrongArgumentException("Format must be specified");
        }
        return (!accepted.contains(format)) ? accepted.get(0) : format;
    }


    private static Map<String, Object> renameChannelHistogramKeys(Map<String, Object> hist) {
        Object channel = hist.get("concreteChannel");
        Object apparentChannel = hist.get("channel");
        hist.put("channel",channel);
        hist.put("apparentChannel", apparentChannel);
        hist.remove("concreteChannel");
        return hist;
    }

    private static String invertColormap(String colormap) {
        if (colormap.charAt(0) == '!') {
            return colormap.substring(1);
        }
        return '!' + colormap;
    }
}