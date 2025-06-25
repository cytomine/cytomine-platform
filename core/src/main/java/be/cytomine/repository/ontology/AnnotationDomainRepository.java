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

import be.cytomine.domain.ontology.AnnotationDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.Tuple;
import java.util.List;

public interface AnnotationDomainRepository extends JpaRepository<AnnotationDomain, Long>, JpaSpecificationExecutor<AnnotationDomain>  {


    @Query(value = "SELECT annotation.id as annotation,user_id as user\n" +
            "FROM user_annotation annotation\n" +
            "WHERE annotation.image_id = :image\n" +
            "AND user_id IN (:layers)\n" +
            "AND ST_Intersects(annotation.location,ST_GeometryFromText(:location,0))", nativeQuery = true)
    List<Tuple> findAllIntersectForUserAnnotations(Long image, List<Long> layers, String location);

    @Query(value = "SELECT annotation.id as annotation,user_id as user\n" +
            "FROM reviewed_annotation annotation\n" +
            "WHERE annotation.image_id = :image\n" +
            "AND ST_Intersects(annotation.location,ST_GeometryFromText(:location,0))", nativeQuery = true)
    List<Tuple> findAllIntersectForReviewedAnnotations(Long image, String location);

    @Query(value = "SELECT count(annotation.id) FROM user_annotation annotation WHERE annotation.project_id = :projectId", nativeQuery = true)
    Long countAllUserAnnotationAndProject(Long projectId);

    @Query(value = "SELECT count(annotation.id) FROM reviewed_annotation annotation WHERE annotation.project_id = :projectId", nativeQuery = true)
    Long countAllReviewedAnnotationAndProject(Long projectId);

}
