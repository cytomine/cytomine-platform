package be.cytomine.repository;

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

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.Track;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public abstract class AnnotationListing {

    /**
     *  default property group to show
     */
    public static final List<String> availableColumnsDefault = List.of("basic", "meta", "term");

    /**
     *  all properties group available, each value is a list of assoc [propertyName, SQL columnName/methodName)
     *  If value start with #, don't use SQL column, its a "trensiant property"
     */
    abstract LinkedHashMap<String, AvailableColumns> getAvailableColumn();

    protected EntityManager entityManager;

    List<String> columnsToPrint;

    Long project = null;
    Long image = null;
    List<Long> images = null;

    Long slice = null;
    List<Long> slices = null;

    Long track = null;
    List<Long> tracks = null;
    Long beforeSlice = null;
    Long afterSlice = null;
    Long sliceDimension = null;

    Long annotationGroup = null;
    List<Long> annotationGroups = null;

    Long user = null;

    Long term = null;
    List<Long> terms = null;

    List<Long> users = null;//for user that draw annotation
    List<Long> usersForTerm = null;//for user that add a term to annotation

    List<Long> reviewUsers;

    Long tag = null;
    List<Long> tags = null;

    Date afterThan = null;
    Date beforeThan = null;

    Boolean notReviewedOnly = false;
    Boolean noTerm = false;
    Boolean noTag = false;
    Boolean multipleTerm = false;
    Boolean noTrack = false;
    Boolean multipleTrack = false;

    String bbox = null;
    String bboxAnnotation = null;

    Object baseAnnotation = null;
    Long maxDistanceBaseAnnotation = null;


    List<Long> parents;

    //not used for search critera (just for specific request
    Boolean avoidEmptyCentroid = false;
    Long excludedAnnotation = null;

    Boolean kmeans = false;
    Integer kmeansValue = 3;

    abstract String getFrom();

    public abstract String getDomainClass();

    abstract String buildExtraRequest();

    LinkedHashMap<String,String> extraColmun = new LinkedHashMap<>();

    LinkedHashMap<String,String> orderBy = new LinkedHashMap<>();

    public AnnotationListing(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void addExtraColumn(String propName, String column) {
        extraColmun.put(propName, column);
    }

    /**
     * Get all properties name available
     * If group argument is provieded, just get properties from these groups
     */
    public List<String> getAllPropertiesName() {
        return getAllPropertiesName(new ArrayList<>(getAvailableColumn().keySet()));
    }

    public List<String> getAllPropertiesName(List<String> groups) {
        List<String> propNames = new ArrayList<>();
        for (String groupName : groups) {
            for (Map.Entry<String, String> assoc : getAvailableColumn().get(groupName).entrySet()) {
                propNames.add(assoc.getKey());
            }
        }
        return propNames;
    }

    /**
     * Get all properties to print
     */
    Map<String, String> buildColumnToPrint() {
        if (columnsToPrint==null) {
            columnsToPrint = new ArrayList<>(availableColumnsDefault);
        }
        columnsToPrint.add("basic"); //mandatory to have id
        columnsToPrint = columnsToPrint.stream().distinct().collect(Collectors.toList());

        Map<String, String> columns = new LinkedHashMap<>();

        for (Map.Entry<String, AvailableColumns> entry : getAvailableColumn().entrySet()) {
            if (columnsToPrint.contains(entry.getKey())) {
                columns.putAll(entry.getValue());
            }
        }
        columns.putAll(extraColmun);
        return columns;
    }

    /**
     * Get container for security check
     */
    public CytomineDomain container() {
        if (project!=null) {
            return entityManager.find(Project.class, project);
        } else if (image!=null) {
            return entityManager.find(ImageInstance.class, image).container();
        } else if (images!=null) {
            List<Project> projectList = new ArrayList<>();
            for (Long idImage : images) {
                projectList.add((Project)entityManager.find(ImageInstance.class, idImage).getProject());
            }
            projectList = projectList.stream().distinct().collect(Collectors.toList());
            if (projectList.size() > 1) {
                throw new WrongArgumentException("Images from filter must all be from the same project!");
            }
            return projectList.stream().findFirst().get();
        } else if (slice!=null) {
            return entityManager.find(SliceInstance.class, slice).container();
        } else if (slices!=null) {
            List<Project> projectList = new ArrayList<>();
            for (Long idImage : slices) {
                projectList.add((Project)entityManager.find(SliceInstance.class, idImage).getProject());
            }
            projectList = projectList.stream().distinct().collect(Collectors.toList());
            if (projectList.size() > 1) {
                throw new WrongArgumentException("Slices from filter must all be from the same project!");
            }
            return projectList.stream().findFirst().get();
        }
        throw new WrongArgumentException("There is no project or image or slice filter. We cannot check acl!");
    }

    /**
     * Generate SQL request string
     */
    public String getAnnotationsRequest() {

        buildExtraRequest();

        Map<String, String> columns = buildColumnToPrint();
        Map<String, String> sqlColumns = new LinkedHashMap<>();
        Map<String, String>  postComputedColumns = new LinkedHashMap<>();

        for (Map.Entry<String, String> colum : columns.entrySet()) {
            if (!colum.getValue().startsWith("#")) {
                sqlColumns.put(colum.getKey(), colum.getValue());
            } else {
                postComputedColumns.put(colum.getKey(), colum.getValue());
            }
        }
        String whereRequest =
                getProjectConst() +
                        getUserConst() +
                        getUsersConst() +

                        getImageConst() +
                        getImagesConst() +

                        getSliceConst() +
                        getSlicesConst() +

                        getTagConst() +
                        getTagsConst() +

                        getTermConst() +
                        getTermsConst() +

                        getTrackConst() +
                        getTracksConst() +
                        getBeforeOrAfterSliceConst() +

                        getGroupConst() +
                        getGroupsConst() +

                        getUsersForTermConst() +

                        getNotReviewedOnlyConst() +
                        getParentsConst() +
                        getAvoidEmptyCentroidConst() +
                        getReviewUsersConst() +

                        getIntersectConst() +
                        getIntersectAnnotationConst() +
                        getMaxDistanceAnnotationConst() +
                        getExcludedAnnotationConst() +

                        getBeforeThan() +
                        getAfterThan() +
                        createOrderBy();

        if (term!=null || terms!=null || track!=null || tracks!=null) {
            String request = "SELECT DISTINCT a.*, ";

            if (term!=null || terms!=null) {
                sqlColumns.remove("term");
                sqlColumns.remove("annotationTerms");
                sqlColumns.remove("userTerm");

                if (this instanceof ReviewedAnnotationListing) {
                    request += "at.term_id as term, 0 as annotationTerms, a.user as userTerm ";
                }
                else {
                    request += "at.term_id as term, at.id as annotationTerms, at.user_id as userTerm ";
                }

            }

            if ((term!=null || terms!=null) && (track!=null || tracks!=null)) {
                request += ", ";
            }

            if (track!=null || tracks!=null) {
                sqlColumns.remove("track");
                sqlColumns.remove("annotationTracks");
                request += "atr.track_id as track, atr.id as annotationTracks ";
            }

            request += "FROM (" + getSelect(sqlColumns) + getFrom() + whereRequest + ") a \n";

            if (term!=null || terms!=null) {
                if (this instanceof ReviewedAnnotationListing) {
                    request += "LEFT OUTER JOIN reviewed_annotation_term at ON a.id = at.reviewed_annotation_terms_id ";
                }
                else {
                    request += "LEFT OUTER JOIN annotation_term at ON a.id = at.user_annotation_id ";
                }
            }


            if (track!=null || tracks!=null)
                request += "LEFT OUTER JOIN annotation_track atr ON a.id = atr.annotation_ident ";

            request += "WHERE true ";
            if (term!=null || terms!=null) {
                 if (!(this instanceof ReviewedAnnotationListing)) {
                    request += "AND at.deleted IS NULL ";
                }
            }

            request += "ORDER BY ";
            request += (track!=null || tracks!=null) ? "a.rank asc" : "a.id desc ";
            if (term!=null || terms!=null) {
                request += ", at.term_id ";
            }
            request += ((track!=null || tracks!=null) ? ", atr.track_id " : "");
            return request;
        }

        return getSelect(sqlColumns) + getFrom() + whereRequest;

    }

    /**
     * Generate SQL string for SELECT with only asked properties
     */
    String getSelect(Map<String,String> columns) {
        if (kmeansValue >= 3) {
            List<String> requestHeadList = new ArrayList<>();
            for (Map.Entry<String, String> entry : columns.entrySet()) {
                if (entry.getKey().equals("term") && !(this instanceof ReviewedAnnotationListing)) {
                    String table ="";
                    if(entry.getValue().contains("aat")) {
                        table = "aat";
                    } else if(entry.getValue().contains("at")) {
                        table = "at";
                    }
                    requestHeadList.add("CASE WHEN "+table+".deleted IS NOT NULL THEN NULL ELSE "+entry.getValue()+" END as " + entry.getKey());
                } else {
                    requestHeadList.add(entry.getValue() + " as " + entry.getKey());
                }
            }
            if (track!=null || tracks!=null) {
                requestHeadList.add("(asl.channel + ai.channels * (asl.z_stack + ai.depth * asl.time)) as rank");
            }
            return "SELECT " + String.join(", ", requestHeadList) + " \n";
        } else {
            return "SELECT ST_ClusterKMeans(location, 5) OVER () AS kmeans, location\n";
        }

    }
    /**
     * Add property group to show if use in where constraint.
     * E.g: if const with term_id = x, we need to make a join on annotation_term.
     * So its mandatory to add "term" group properties (even if not asked)
     */
    void addIfMissingColumn(String column) {
        if (!columnsToPrint.contains(column)) {
            columnsToPrint.add(column);
        }
    }

    String joinValues(List list) {
        return (String)list.stream().map(x -> String.valueOf(x)).collect(Collectors.joining(", "));
    }

    String getProjectConst() {
        return (project!=null ? "AND a.project_id = " + project + "\n" : "");
    }

    String getUsersConst() {
        return (users!=null ? "AND a.user_id IN ("+joinValues(users)+")\n" : "");
    }

    String getReviewUsersConst() {
        return (reviewUsers!=null ? "AND a.review_user_id IN ("+joinValues(reviewUsers)+")\n" : "");
    }


    String getUsersForTermConst() {
        if (usersForTerm!=null) {
            addIfMissingColumn("term");
            return "AND at.user_id IN ("+joinValues(usersForTerm)+")\n";
        } else {
            return "";
        }
    }

    String getImagesConst() {
        if (images!=null && project!=null && images.size() == entityManager.find(Project.class, project).getCountImages()) {
            return ""; //images number equals to project image number, no const needed
        } else if (images!=null && images.isEmpty()) {
            throw new ObjectNotFoundException("The image has been deleted!");
        } else {
            return (images!=null ? "AND a.image_id IN ("+joinValues(images)+")\n" : "");
        }

    }

    String getImageConst() {
        if (image!=null) {
            ImageInstance imageInstance = entityManager.find(ImageInstance.class, image);
            if (imageInstance==null) {
                throw new ObjectNotFoundException("Image " + image + " not exist!");
            }
            return "AND a.image_id = " + imageInstance.getId() + "\n";
        } else {
            return "";
        }
    }

    String getSlicesConst() {
        if (slices!=null && slices.isEmpty()) {
            throw new ObjectNotFoundException("The slice has been deleted!");
        } else {
            return (slices!=null ? "AND a.slice_id IN ("+joinValues(slices)+")\n" : "");
        }

    }

    String getSliceConst() {
        if (slice!=null) {
            if (entityManager.find(SliceInstance.class, slice)==null) {
                throw new ObjectNotFoundException("Slice "+slice+" not exist!");
            }
            return "AND a.slice_id = " + slice + "\n";
        } else {
            return "";
        }
    }

    String getUserConst() {
        if (user!=null) {
            if (entityManager.find(User.class, user)==null) {
                throw new ObjectNotFoundException("User "+user+" not exist!");
            }
            return "AND a.user_id = "+user+"\n";
        } else {
            return "";
        }
    }

    abstract String getNotReviewedOnlyConst();

    String getIntersectConst() {
        return (bbox!=null ? "AND ST_Intersects(a.location,ST_GeometryFromText('"+bbox.toString()+"',0))\n" : "");
    }

    String getIntersectAnnotationConst() {
        return (bboxAnnotation!=null ? "AND ST_Intersects(a.location,ST_GeometryFromText('" + bboxAnnotation.toString() + "',0))\n" : "");
    }

    String getMaxDistanceAnnotationConst() {
        if(maxDistanceBaseAnnotation!=null) {
            if(baseAnnotation==null) {
                throw new ObjectNotFoundException("You need to provide a 'baseAnnotation' parameter (annotation id/location = "+baseAnnotation+")!");
            } else {
                try {
                    AnnotationDomain base = AnnotationDomain.getAnnotationDomain(entityManager, ((Long)baseAnnotation), null);
                    //ST_distance(a.location,ST_GeometryFromText('POINT (0 0)'))
                    return "AND ST_distance(a.location,ST_GeometryFromText('"+base.getWktLocation() + "')) <= "+maxDistanceBaseAnnotation+"\n";
                } catch (Exception e) {
                    return "AND ST_distance(a.location,ST_GeometryFromText('"+baseAnnotation+ "')) <= " + maxDistanceBaseAnnotation + "\n";
                }
            }
        } else {
            return "";
        }
    }

    String getAvoidEmptyCentroidConst() {
        return (avoidEmptyCentroid ? "AND ST_IsEmpty(st_centroid(a.location))=false\n" : "");
    }

    String getTermConst() {
        if (term!=null) {
            if (entityManager.find(Term.class, term)==null) {
                throw new ObjectNotFoundException("Term " + term + "not exist!");
            }
            addIfMissingColumn("term");

            if (this instanceof ReviewedAnnotationListing)
                return " AND (at.term_id = "+term + ((noTerm) ? " OR at.term_id IS NULL" : "") + ")\n";
            else
                return " AND ((at.term_id = "+term+ ")" + ((noTerm) ? " OR at.term_id IS NULL" : "") + ")\n";
        } else {
            return "";
        }
    }
    String getParentsConst() {
        if (parents!=null) {
            return " AND a.parent_ident IN ("+joinValues(parents)+")\n";
        } else {
            return "";
        }
    }


    String getTermsConst() {
        if (terms!=null) {
            addIfMissingColumn("term");
            if (this instanceof ReviewedAnnotationListing)
                return " AND (at.term_id IN ("+joinValues(terms) + ")" + ((noTerm) ? " OR at.term_id IS NULL" : "") + ")\n";
            else
                return " AND ((at.term_id IN ("+joinValues(terms) + "))" + ((noTerm) ? " OR at.term_id IS NULL" : "") + ")\n";
        } else {
            return "";
        }
    }

    String getTrackConst() {
        if (track!=null) {
            if (entityManager.find(Track.class, track)!=null) {
                throw new ObjectNotFoundException("Track " + track + " not exists !");
            }
            addIfMissingColumn("track");
            return " AND (atr.track_id = "+track + ((noTrack) ? " OR atr.track_id IS NULL" : "") + ")\n";
        } else {
            return "";
        }
    }

    String getTracksConst() {
        if (tracks!=null) {
            addIfMissingColumn("track");
            return "AND (atr.track_id IN ("+joinValues(tracks) +") " + ((noTrack) ? " OR atr.track_id IS NULL" : "") + ")\n";
        } else {
            return "";
        }
    }

    String getGroupConst() {
        if (annotationGroup == null) {
            return "";
        }

        if (entityManager.find(AnnotationGroup.class, annotationGroup) != null) {
            throw new ObjectNotFoundException("Annotation group  " + annotationGroup + " does not exists!");
        }

        addIfMissingColumn("group");
        return " AND al1.group_id = " + annotationGroup + "\n";
    }

    String getGroupsConst() {
        if (annotationGroups == null) {
            return "";
        }

        addIfMissingColumn("group");
        return " AND al1.group_id IN (" + joinValues(annotationGroups) + ")\n";
    }

    String getTagConst() {
        if (tag!=null && noTag) {
            return "AND (tda.tag_id = "+tag + " OR tda.tag_id IS NULL)\n";
        } else if (tag!=null) {
            return "AND tda.tag_id = "+tag +"\n";
        } else {
            return "";
        }
    }

    String getTagsConst() {
        if (tags!=null  && noTag) {
            return "AND (tda.tag_id IN ("+joinValues(tags) + ") OR tda.tag_id IS NULL)\n";
        } else if (tags!=null ) {
            return "AND tda.tag_id IN (" + joinValues(tags) + ")\n";
        } else {
            return "";
        }
    }


        String getBeforeOrAfterSliceConst() {
        if ((track!=null || tracks!=null) && (beforeSlice!=null || afterSlice!=null)) {
            addIfMissingColumn("slice");
            Long sliceId = (beforeSlice!=null) ? beforeSlice : afterSlice;
            SliceInstance sliceInstance = entityManager.find(SliceInstance.class, sliceId);
            if (sliceInstance==null) {
                throw new ObjectNotFoundException("Slice "+ sliceId +" not exists !");
            }
            String sign = (beforeSlice!=null) ? "<" : ">";
            return "AND (asl.channel + ai.channels * (asl.z_stack + ai.depth * asl.time)) "+sign+" " + sliceInstance.getBaseSlice().getRank() +"\n";
        } else {
            return "";
        }
    }

    String getExcludedAnnotationConst() {
        return (excludedAnnotation!=null ? "AND a.id <> " + excludedAnnotation + "\n" : "");
    }

    abstract String createOrderBy();

    String getBeforeThan() {
        if (beforeThan!=null) {
            return "AND a.created < '"+beforeThan+"'\n";
        } else {
            return "";
        }
    }
    String getAfterThan() {
        if (afterThan!=null) {
            return "AND a.created > '"+afterThan+"'\n";
        } else {
            return "";
        }
    }
}


