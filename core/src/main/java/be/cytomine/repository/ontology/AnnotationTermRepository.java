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

import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AnnotationTermRepository extends JpaRepository<AnnotationTerm, Long>, JpaSpecificationExecutor<AnnotationTerm>  {
    List<AnnotationTerm> findAllByUserAnnotation(UserAnnotation annotation);
    List<AnnotationTerm> findAllByUserAnnotationId(Long annotation);


    Optional<AnnotationTerm> findByUserAnnotationAndTermAndUser(UserAnnotation annotation, Term term, User user);


    Optional<AnnotationTerm>  findByUserAnnotationIdAndTermIdAndUserId(Long annotation, Long term, Long user);

    List<AnnotationTerm> findAllByUserAndUserAnnotation(User user, UserAnnotation annotation);

    long countByTerm(Term term);

    List<AnnotationTerm> findAllByUserAnnotation_Project(Project project);

    List<AnnotationTerm> findAllByUser(User user);
}
