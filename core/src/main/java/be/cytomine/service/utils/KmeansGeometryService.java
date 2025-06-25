package be.cytomine.service.utils;

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

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.User;
import be.cytomine.dto.Kmeans;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.SliceInstanceRepository;
import be.cytomine.service.ontology.AnnotationIndexService;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class KmeansGeometryService {

    private final AnnotationIndexService annotationIndexService;

    private final EntityManager entityManager;

    private final SliceInstanceRepository sliceInstanceRepository;

    public static final int FULL = 3;
    public static final int KMEANSFULL = 2;
    public static final int KMEANSSOFT = 1;

    public static final int ANNOTATIONSIZE1 = 0;
    public static final int ANNOTATIONSIZE2 = 100;
    public static final int ANNOTATIONSIZE3 = 2000;
    public static final int ANNOTATIONSIZE4 = 10000;
    public static final int ANNOTATIONSIZE5 = 100000;


    public static final Map<Integer, Map<Integer, Integer>> rules =
        Map.of(
                100, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, KMEANSFULL, (int)ANNOTATIONSIZE4, KMEANSFULL, (int)ANNOTATIONSIZE5, KMEANSFULL),
                75, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, FULL, (int)ANNOTATIONSIZE4, KMEANSFULL, (int)ANNOTATIONSIZE5, KMEANSFULL),
                50, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, FULL, (int)ANNOTATIONSIZE4, FULL, (int)ANNOTATIONSIZE5, KMEANSFULL),
                25, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, FULL, (int)ANNOTATIONSIZE4, FULL, (int)ANNOTATIONSIZE5, FULL),
                0, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, FULL, (int)ANNOTATIONSIZE4, FULL, (int)ANNOTATIONSIZE5, FULL)

        );


    public List<Kmeans> doKeamsFullRequest(String request) {
        String requestKmeans = "SELECT kmeans, count(*), st_astext(ST_ConvexHull(ST_Collect(location))) \n" +
                "FROM (\n" + request +"\n" +") AS ksub\n" +
                "GROUP BY kmeans\n" +
                "ORDER BY kmeans;";
        return selectAnnotationLightKmeans(requestKmeans);
    }

    public List<Kmeans> doKeamsSoftRequest(String request) {
        String requestKmeans = "SELECT kmeans, count(*), st_astext(ST_Centroid(ST_Collect(location))) \n" +
                "FROM (\n" + request +"\n" +") AS ksub\n" +
                "GROUP BY kmeans\n" +
                "ORDER BY kmeans;";
        return selectAnnotationLightKmeans(requestKmeans);
    }

    private List<Kmeans> selectAnnotationLightKmeans(String request) {
        List<Kmeans> data = new ArrayList<>();

        double max = 1;

        Query nativeQuery = entityManager.createNativeQuery(request, Tuple.class);
        List<Tuple> resultList = nativeQuery.getResultList();
        for (Tuple tuple : resultList) {
            Kmeans kmeans = new Kmeans();
            kmeans.setId(((Integer)tuple.get(0)).longValue());
            kmeans.setCount((Long)tuple.get(1));
            kmeans.setLocation((String) tuple.get(2));
            if(kmeans.getCount()>max) {
                max = kmeans.getCount();
            }
            data.add(kmeans);
        }

        for (Kmeans datum : data) {
            datum.setRatio(((double)datum.getCount()/max));
        }
        return data;
    }

    public int mustBeReduce(List<Long> slices, Long userId, String bbox) {
        List<SliceInstance> sliceInstances = sliceInstanceRepository.findAllById(slices);

        User user = null;
        if (userId !=null) {
            user = entityManager.find(User.class, userId);
        }
        try {
            return mustBeReduce(sliceInstances, user,new WKTReader().read(bbox));
        } catch (ParseException e) {
            throw new WrongArgumentException("Annotation location cannot be converted to geometry: " + bbox);
        }
    }


    public int mustBeReduce(List<SliceInstance> slices, User user, Geometry bbox) {
        List<ImageInstance> images = slices.stream().map(x -> x.getImage()).distinct().toList();

        if (images.size() != 1) {
            throw new WrongArgumentException("To use kmeans, all slices must belong to the same image.");
        }
        ImageInstance image = images.get(0);

        if (image.getBaseImage().getWidth()==null) {
            return  FULL;
        }

        double imageWidth = image.getBaseImage().getWidth();
        double bboxWidth = bbox.getEnvelopeInternal().getWidth();

        double ratio = bboxWidth/imageWidth;

        int ratio25 = ((int)((ratio/25d)*100))*25;

        Map<Integer, Integer> ruleLine = rules.get(Math.min(ratio25,100));

        long numberOfAnnotation = Math.max(0, annotationIndexService.count(slices,user));

        Integer rule = getRuleForNumberOfAnnotations(numberOfAnnotation, ruleLine);
        return rule;
    }

    public Integer getRuleForNumberOfAnnotations(long annotations, Map<Integer, Integer> ruleLine) {
        if (annotations >= ANNOTATIONSIZE5) return ruleLine.get(ANNOTATIONSIZE5);
        if (annotations >= ANNOTATIONSIZE4) return ruleLine.get(ANNOTATIONSIZE4);
        if (annotations >= ANNOTATIONSIZE3) return ruleLine.get(ANNOTATIONSIZE3);
        if (annotations >= ANNOTATIONSIZE2) return ruleLine.get(ANNOTATIONSIZE2);
        if (annotations >= ANNOTATIONSIZE1) return ruleLine.get(ANNOTATIONSIZE1);
        throw new WrongArgumentException("Cannot find rule for annotations count = " + annotations);
    }



}
