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

import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.ontology.AnnotationLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnotationLinkRepository extends JpaRepository<AnnotationLink, Long>, JpaSpecificationExecutor<AnnotationLink> {

    Optional<AnnotationLink> findByAnnotationIdent(Long id);

    Optional<AnnotationLink> findByAnnotationIdentAndGroup(Long id, AnnotationGroup group);

    List<AnnotationLink> findAllByGroup(AnnotationGroup group);

    void deleteAllByGroup(AnnotationGroup group);

    @Modifying
    @Query(value = "UPDATE AnnotationLink al SET al.group = :newGroup WHERE al.group = :mergedGroup")
    void setMergedAnnotationGroup(AnnotationGroup newGroup, AnnotationGroup mergedGroup);
}
