package be.cytomine.repository.ontology;

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
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.annotation.AnnotationLight;
import be.cytomine.service.UrlApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public interface UserAnnotationRepository extends JpaRepository<UserAnnotation, Long>, JpaSpecificationExecutor<UserAnnotation> {

    List<UserAnnotation> findAllByProjectAndUserIdIn(Project project, List<Long> layers);

    List<UserAnnotation> findAllByUserAndImage(User user, ImageInstance imageInstance);

    Long countByProject(Project project);

    Long countByUserAndProject(User user, Project project);

    Long countByUser(User user);

    Long countByProjectAndCreatedAfter(Project project, Date createdMin);

    Long countByProjectAndCreatedBefore(Project project, Date createdMax);

    Long countByProjectAndCreatedBetween(Project project, Date createdMin, Date createdMax);


    @Query(
            value = "SELECT a.id id, a.project_id container, '' url FROM user_annotation a, image_instance ii, abstract_image ai WHERE a.image_id = ii.id AND ii.base_image_id = ai.id AND ai.original_filename not like '%ndpi%svs%' AND GeometryType(a.location) != 'POINT' AND st_area(a.location) < 1500000 ORDER BY st_area(a.location) DESC",
            nativeQuery = true)
    List<Tuple> listTuplesLight();

    default List<AnnotationLight> listLight() {
        List<AnnotationLight> annotationLights = new ArrayList<>();
        for (Tuple tuple : listTuplesLight()) {
            annotationLights.add(new AnnotationLight(
                    (Long)tuple.get("id"),
                    (Long)tuple.get("container"),
                    UrlApi.getUserAnnotationCropWithAnnotationIdWithMaxSize((Long)tuple.get("id"), 256, "png"))
            );
        }
        return annotationLights;
    }


    @Query(value = "SELECT a.id id, ST_distance(a.location,ST_GeometryFromText(:geometry))  FROM user_annotation a WHERE project_id = :projectId", nativeQuery = true)
    List<Tuple> listAnnotationWithDistance(Long projectId, String geometry);


    List<UserAnnotation> findAllByImage(ImageInstance image);

    List<UserAnnotation> findAllByUser(User user);
}
