package be.cytomine.utils;

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

import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.dto.annotation.AnnotationResult;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.*;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.report.ReportService;
import be.cytomine.service.security.UserService;
import be.cytomine.service.utils.ParamsService;

import org.locationtech.jts.io.ParseException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class AnnotationListingBuilder {

    private final UserService userService;

    private final EntityManager entityManager;

    private final ParamsService paramsService;

    private final TermService termService;

    private final AnnotationListingService annotationListingService;

    private final ReportService reportService;

    private final ProjectService projectService;


    public byte[] buildAnnotationReport(Long project, String users, JsonObject params, String terms, String format){
        List<Map<String, Object>> annotations = buildAnnotationList(params, users);
        Set<String> termNames = getTermNames(terms);
        Set<String> userNames = getUserNames(users);
        return reportService.generateAnnotationsReport(projectService.get(project).getName(), termNames, userNames, annotations, format);
    }

    public List<Map<String, Object>> buildAnnotationList(JsonObject params, String users){
        AnnotationListing annotationListing = buildAnnotationListing(params);
        annotationListing.getColumnsToPrint().add("gis");
        annotationListing.getColumnsToPrint().add("image");
        annotationListing.getColumnsToPrint().add("user");
        return filterAnnotationByUsers(annotationListingService.listGeneric(annotationListing), users);
    }

    private List<Map<String, Object>> filterAnnotationByUsers(List<AnnotationResult> annotations, String users){
        List<Map<String, Object>> filteredAnnotations = new ArrayList<>();
        List<Long> userIds = Arrays.stream(users.split(","))
                .sequential()
                .filter(id -> !id.isEmpty())
                .map(id -> Long.parseLong(id))
                .collect(Collectors.toList());

        for(AnnotationResult annotation : annotations){
            if(userIds.contains((long)annotation.get("user"))){
                filteredAnnotations.add(annotation);
            }
        }

        return filteredAnnotations;
    }


    public AnnotationListing buildAnnotationListing(JsonObject params) {
        AnnotationListing al;
        if(isReviewedAnnotationAsked(params)) {
            al = new ReviewedAnnotationListing(entityManager);
        }
        else {
            al = new UserAnnotationListing(entityManager);
        }

        return buildAnnotationListing(al, params);
    }

    public AnnotationListing buildAnnotationListing(AnnotationListing al, JsonObject params) {
        al.setColumnsToPrint(paramsService.getPropertyGroupToShow(params));

        // Project
        al.setProject(params.getJSONAttrLong("project"));

        // Images
        al.setImage(params.getJSONAttrLong("image"));
        al.setImages(StringUtils.extractListFromParameter(params.getJSONAttrStr("images")));

        // Slices
        al.setSlice(params.getJSONAttrLong("slice"));
        al.setSlices(StringUtils.extractListFromParameter(params.getJSONAttrStr("slices")));

        // Tracks
        al.setTrack(params.getJSONAttrLong("track"));
        al.setTracks(StringUtils.extractListFromParameter(params.getJSONAttrStr("tracks")));

        if (al.getTrack()!=null || al.getTracks()!=null) {
            al.setBeforeSlice(params.getJSONAttrLong("beforeSlice"));
            al.setAfterSlice(params.getJSONAttrLong("afterSlice"));
            al.setSliceDimension(params.getJSONAttrLong("sliceDimension"));
        }

        // Users
        al.setUser(params.getJSONAttrLong("user"));
        al.setUsers(StringUtils.extractListFromParameter(params.getJSONAttrStr("users")));

        al.setUsersForTerm(StringUtils.extractListFromParameter(params.getJSONAttrStr("usersForTerm")));

        // Tags
        al.setTag(params.getJSONAttrLong("tag"));
        al.setTags(StringUtils.extractListFromParameter(params.getJSONAttrStr("tags")));
        al.setNoTag(params.getJSONAttrBoolean("noTag", false));

        // Terms
        al.setTerm(params.getJSONAttrLong("term"));
        al.setTerms(StringUtils.extractListFromParameter(params.getJSONAttrStr("terms")));

        // Boolean for terms
        al.setNoTerm(params.getJSONAttrBoolean("noTerm", false));
        al.setMultipleTerm(params.getJSONAttrBoolean("multipleTerm", false));
        al.setNoTrack(params.getJSONAttrBoolean("noTrack", false));
        al.setMultipleTrack(params.getJSONAttrBoolean("multipleTrack", false));

        // Review
        al.setNotReviewedOnly(params.getJSONAttrBoolean("notReviewedOnly", false));

        // Review users
        // TODO: reviewUser ?
        al.setReviewUsers(StringUtils.extractListFromParameter(params.getJSONAttrStr("reviewUsers")));

        // Kmeans
        al.setKmeans(params.getJSONAttrBoolean("kmeans", false));
        al.setKmeansValue(params.getJSONAttrInteger("kmeansValue", null));

        // BBOX
        if(params.get("bbox")!=null) {
            try {
                al.setBbox(GeometryUtils.createBoundingBox(params.getJSONAttrStr("bbox")).toText());
            } catch (ParseException e) {
                throw new WrongArgumentException("Geometry cannot be parsed for annotation search request:" + e);
            }
        }
        if(params.get("bboxAnnotation")!=null) {
            AnnotationDomain annotationDomain = AnnotationDomain.getAnnotationDomain(entityManager, params.getJSONAttrLong("bboxAnnotation"));
            al.setBboxAnnotation(annotationDomain.getWktLocation());
        }

        // Base annotation
        al.setBaseAnnotation(params.getJSONAttrLong("baseAnnotation")!=null?
                params.getJSONAttrLong("baseAnnotation") : // can be an annotation id
                params.getJSONAttrStr("baseAnnotation")); // can be a string (wkt) too
        al.setMaxDistanceBaseAnnotation(params.getJSONAttrLong("maxDistanceBaseAnnotation"));

        // Date
        al.setAfterThan(params.getJSONAttrDate("afterThan"));
        al.setBeforeThan(params.getJSONAttrDate("beforeThan"));

        al.setExcludedAnnotation(params.getJSONAttrLong("excludedAnnotation")); // TODO ?

        return al;
    }

    /**
     * Check if we ask reviewed annotation
     */
    private boolean isReviewedAnnotationAsked(JsonObject params) {
        return params.getJSONAttrBoolean("reviewed", false);
    }

    /**
     * From a string representing the list of terms ids, get a set of terms name.
     */
    public Set<String> getTermNames(String terms){
        Set<String> termNames = new HashSet<>();
        for (String termId : terms.split(",")){
            if(!termId.equals("0") && !termId.equals("-1") && !termId.isEmpty()){
                termNames.add(termService.find(Long.parseLong(termId)).get().getName());
            }
        }
        return termNames;
    }

    /**
     * From a string representing the list of users ids, get a set of users name.
     */
    public Set<String> getUserNames(String users) {
        Set<String> userNames = new HashSet<>();
        for (String userId : users.split(",")){
            if(!userId.isEmpty()){
                userNames.add(userService.get(Long.parseLong(userId)).getUsername());
            }
        }
        return userNames;
    }
}
