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
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.ReviewedAnnotationStatsEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;


public interface ReviewedAnnotationRepository extends JpaRepository<ReviewedAnnotation, Long>, JpaSpecificationExecutor<ReviewedAnnotation>  {

    Long countByProject(Project project);

    Long countByUser(User user);

    Long countByProjectAndCreatedAfter(Project project, Date createdMin);

    Long countByProjectAndCreatedBefore(Project project, Date createdMax);

    Long countByProjectAndCreatedBetween(Project project, Date createdMin, Date createdMax);

    @Query(
            value = "SELECT user_id, count(*), sum(count_reviewed_annotations) as total \n" +
                    "FROM user_annotation ua\n" +
                    "WHERE ua.image_id = :imageId\n" +
                    "GROUP BY user_id\n" +
                    "ORDER BY total desc;", nativeQuery = true
    )
    List<Tuple> stats(Long imageId);

    @Query(
            value = "SELECT count_reviewed_annotations as total \n" +
                    "FROM user_annotation ua\n" +
                    "WHERE ua.id = :userAnnotationId\n", nativeQuery = true
    )
    long countReviewedAnnotation(Long userAnnotationId);

    default List<ReviewedAnnotationStatsEntry> stats(ImageInstance imageInstance) {
        List<ReviewedAnnotationStatsEntry> reviewedAnnotationStatsEntries = new ArrayList<>();
        for (Tuple tuple : stats(imageInstance.getId())) {
            reviewedAnnotationStatsEntries.add(new ReviewedAnnotationStatsEntry(
                    (Long)tuple.get(0),
                    (Long)tuple.get(1),
                    (Long)tuple.get(2))
            );
        }
        return reviewedAnnotationStatsEntries;
    }

    Optional<ReviewedAnnotation> findByParentIdent(Long parentIdent);

    List<ReviewedAnnotation> findAllByImage(ImageInstance image);

    long countAllByTermsContaining(Term term);

    long countAllByProjectAndTerms_Empty(Project project);

    List<ReviewedAnnotation>  findAllByUser(User user);
}
