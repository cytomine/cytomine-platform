package be.cytomine.service;

import be.cytomine.dto.annotation.AnnotationResult;
import be.cytomine.dto.image.Point;

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

import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.AnnotationListing;
import be.cytomine.repository.UserAnnotationListing;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.KmeansGeometryService;
import be.cytomine.utils.GisUtils;
import be.cytomine.utils.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.transaction.Transactional;
import java.math.BigInteger;
import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Transactional
@Service
public class AnnotationListingService {

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private KmeansGeometryService kmeansGeometryService;


    public List listGeneric(AnnotationListing al) {
        securityACLService.check(al.container(), READ);
        if ((al.getKmeans() != null && al.getKmeans()) && al.getKmeansValue() == null) {
            if (al.getBbox() == null) {
                throw new WrongArgumentException("If you want to use kmeans, you must provide image bbox:" + al.getBbox());
            }

            if (al.getSlice() == null && (al.getSlices() == null || al.getSlices().isEmpty())) {
                throw new WrongArgumentException("If you want to use kmeans, you must provide slice Id");
            }
            al.setSlices((al.getSlices() == null || al.getSlices().isEmpty()) ? List.of(al.getSlice()) : al.getSlices());
            Integer rule = kmeansGeometryService.mustBeReduce(al.getSlices(), al.getUser(), al.getBbox());
            al.setKmeansValue(rule);
        } else if (al.getKmeans() != null && !al.getKmeans()) {
            //no kmeans
            al.setKmeansValue(KmeansGeometryService.FULL);
        }
        return executeRequest(al);
    }

    public List executeRequest(AnnotationListing al) {
        if (al.getKmeansValue() == KmeansGeometryService.FULL) {
            return selectGenericAnnotation(al);
        } else if (al.getKmeansValue() == KmeansGeometryService.KMEANSFULL) {
            return kmeansGeometryService.doKeamsFullRequest(al.getAnnotationsRequest());
        } else {
            return kmeansGeometryService.doKeamsSoftRequest(al.getAnnotationsRequest());
        }
    }

    /**
     * Execute request and format result into a list of map
     */
    private List<AnnotationResult> selectGenericAnnotation(AnnotationListing al) {

        List<AnnotationResult> data = new ArrayList<>();
        long lastAnnotationId = -1;
        long lastTermId = -1;
        long lastTrackId = -1;
        long lastGroupId = -1;

        boolean first = true;

        List<String> realColumn = new ArrayList<>();
        String request = al.getAnnotationsRequest();

        boolean termAsked = false;
        boolean trackAsked = false;
        boolean groupAsked = false;
        List<Long> annotationLinksIds = new ArrayList<>();

        List<String> excludedColumns = List.of(
                "annotationTerms", "annotationTracks", "userTerm", "x", "y",
                "annotationLinks", "linkedAnnotations", "linkedImages", "linkedUpdated"
        );


        Query nativeQuery = entityManager.createNativeQuery(request, Tuple.class);
        List<Tuple> resultList = nativeQuery.getResultList();

        for (Tuple rowResult : resultList) {
            Map<String, Object> tuple = new LinkedHashMap<>();
            for (TupleElement<?> element : rowResult.getElements()) {
                Object value = rowResult.get(element.getAlias());
                if (value instanceof BigInteger) {
                    value = ((BigInteger) value).longValue();
                }
                tuple.put(element.getAlias(), value);
            }

            /**
             * If an annotation has n multiple term, it will be on "n" lines.
             * For the first line for this annotation (it.id!=lastAnnotationId), add the annotation data,
             * For the other lines, we add term data to the last annotation
             */
            if ((Long) tuple.get("id") != lastAnnotationId) {
                termAsked = false;
                trackAsked = false;
                groupAsked = false;
                annotationLinksIds = new ArrayList<>();

                if (first) {
                    for (String columnName : al.getAllPropertiesName()) {
                        if (columnExists(tuple, columnName) && !excludedColumns.contains(columnName)) {
                            realColumn.add(columnName);
                        }
                    }
                    first = false;
                }

                AnnotationResult item = new AnnotationResult();
                item.put("class", al.getDomainClass());

                for (String columnName : realColumn) {
                    item.put(columnName, tuple.get(columnName) != null ? tuple.get(columnName) : tuple.get(columnName.toLowerCase()));
                }


                if (al.getColumnsToPrint().contains("term")) {
                    termAsked = true;
                    item.put("term", tuple.get("term") != null ? buildList(tuple.get("term")) : new ArrayList<>());
                    item.put("userByTerm",
                            tuple.get("term") != null ?
                                    buildList(new HashMap<>(JsonObject.of("id", tuple.get("annotationterms"), "term", tuple.get("term"), "user", buildList(tuple.get("userterm"))))) : new ArrayList<>());
                }

                if (al.getColumnsToPrint().contains("track") && (al instanceof UserAnnotationListing)) {
                    trackAsked = true;
                    item.put("track", (tuple.get("track") != null ? buildList(tuple.get("track")) : new ArrayList<>()));
                    item.put("annotationTrack", (tuple.get("track") != null ? buildList(new HashMap<>(Map.of("id", tuple.get("annotationtracks"), "track", tuple.get("track")))) : new ArrayList<>()));
                }

                if (al.getColumnsToPrint().contains("group")) {
                    groupAsked = true;
                    item.put("group", tuple.get("group"));
                    item.put("annotationLink", (tuple.get("group") != null ? buildList(new HashMap<>(Map.of(
                            "id", tuple.get("annotationlinks"),
                            "annotation", tuple.get("linkedannotations"),
                            "image", tuple.get("linkedimages"),
                            "updated", tuple.get("linkedupdated")
                    ))) : new ArrayList<>()));
                    annotationLinksIds.add((Long) tuple.get("annotationLinks"));
                }

                if (al.getColumnsToPrint().contains("gis")) {
                    item.put("perimeterUnit", tuple.get("perimeterunit") != null ? GisUtils.retrieveUnit((Integer) tuple.get("perimeterunit")) : null);
                    item.put("areaUnit", tuple.get("areaunit") != null ? GisUtils.retrieveUnit((Integer) tuple.get("areaunit")) : null);
                    item.put("centroid", new Point((Double) tuple.get("x"), (Double) tuple.get("y")));
                }

                if(al.getColumnsToPrint().contains("meta")) {
                    if(al.getClass().getName().contains("UserAnnotation")) {
                        item.put("cropURL",UrlApi.getUserAnnotationCropWithAnnotationId((Long)tuple.get("id"), "png"));
                        item.put("smallCropURL",UrlApi.getUserAnnotationCropWithAnnotationIdWithMaxSize((Long)tuple.get("id"), 256, "png"));
                        item.put("url",UrlApi.getUserAnnotationCropWithAnnotationId((Long)tuple.get("id"), "png"));
                        item.put("imageURL",UrlApi.getAnnotationURL((Long)tuple.get("project"), (Long)tuple.get("image"), (Long)tuple.get("id")));
                    }  else if(al.getClass().getName().contains("ReviewedAnnotation")) {
                        item.put("cropURL",UrlApi.getReviewedAnnotationCropWithAnnotationId((Long)tuple.get("id"), "png"));
                        item.put("smallCropURL",UrlApi.getReviewedAnnotationCropWithAnnotationIdWithMaxSize((Long)tuple.get("id"), 256, "png"));
                        item.put("url",UrlApi.getReviewedAnnotationCropWithAnnotationId((Long)tuple.get("id"), "png"));
                        item.put("imageURL",UrlApi.getAnnotationURL((Long)tuple.get("project"), (Long)tuple.get("image"), (Long)tuple.get("id")));
                    }
                }
                data.add(item);
            } else {
                AnnotationResult lastResult = data.get(data.size() - 1);
                if (termAsked && tuple.get("term") != null) {
                    List lasResultUserByTerm = (List) lastResult.get("userByTerm");
                    Map userByTerm = (Map) (lasResultUserByTerm).get(lasResultUserByTerm.size() - 1);
                    List term = ((List) lastResult.get("term"));
                    if ((Long) tuple.get("term") == lastTermId) {
                        if (!((List) (userByTerm.get("user"))).contains(tuple.get("userterm"))) {
                            ((List) (userByTerm.get("user"))).add(tuple.get("userterm"));
                        }
                    } else if (!term.contains(tuple.get("term"))) {
                        ((List) (lastResult.get("term"))).add(tuple.get("term"));
                        ((List) lastResult.get("userByTerm")).add(new HashMap<>(Map.of("id", tuple.get("annotationterms"), "term", tuple.get("term"), "user", buildList(tuple.get("userterm")))));
                    }
                }

                if (trackAsked && tuple.get("track") != null && (long) tuple.get("track") != lastTrackId && !((List) lastResult.get("track")).contains(tuple.get("track"))) {
                    ((List) lastResult.get("track")).add(tuple.get("track"));
                    ((List) lastResult.get("annotationTrack")).add(new HashMap<>(Map.of("id", tuple.get("annotationtracks"), "track", tuple.get("track"))));
                }

                if (groupAsked && tuple.get("group") != null
                        && (long) tuple.get("group") == lastGroupId
                        && !annotationLinksIds.contains(tuple.get("annotationlinks"))
                ) {
                    annotationLinksIds.add((Long) tuple.get("annotationlinks"));
                    ((List) lastResult.get("annotationLink")).add(new HashMap<>(Map.of(
                            "id", tuple.get("annotationlinks"),
                            "annotation", tuple.get("linkedannotations"),
                            "image", tuple.get("linkedimages"),
                            "updated", tuple.get("linkedupdated")
                    )));
                }
            }

            if (termAsked) {
                lastTermId = (tuple.get("term") != null ? (long) tuple.get("term") : -1);
            }

            if (trackAsked) {
                lastTrackId = (tuple.get("track") != null ? (long) tuple.get("track") : -1);
            }

            if (groupAsked) {
                lastGroupId = (tuple.get("group") != null ? (long) tuple.get("group") : -1);
            }

            lastAnnotationId = (long) tuple.get("id");
        }

        return data;
    }

    private boolean columnExists(Map<String, Object> tuple, String columnName) {
        return tuple.get(columnName) != null || tuple.get(columnName.toLowerCase()) != null;
    }

    List buildList(Object firstElement) {
        if (firstElement==null) {
            return new ArrayList();
        }
        List list = new ArrayList();
        list.add(firstElement);
        return list;
    }
}
