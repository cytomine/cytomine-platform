package be.cytomine.repository.project;

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

import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.DatedCytomineDomain;
import be.cytomine.dto.NamedCytomineDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.Tuple;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>  {

    @Query(value = "select distinct project "+
            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, Project as project "+
            "where aclObjectId.objectId = project.id " +
            "and aclEntry.aclObjectIdentity = aclObjectId "+
            "and project.ontology = :ontology " +
            "and aclEntry.sid = aclSid and aclSid.sid like :username")
    List<Project> findAllProjectForUserByOntology(String username, Ontology ontology);


    @Query(value = "select distinct project "+
            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, Project as project "+
            "where aclObjectId.objectId = project.id " +
            "and aclEntry.aclObjectIdentity = aclObjectId "+
            "and aclEntry.sid = aclSid and aclSid.sid like :username")
    List<Project> findAllProjectForUser(String username);




    @Query(value = "select id, name from creator_project where user_id = :userId", nativeQuery = true)
    List<Tuple> listByCreator(Long userId);

    default List<NamedCytomineDomain> listByCreator(User user) {
        return listByCreator(user.getId()).stream().map(x -> new NamedCytomineDomain(((Long)x.get(0)), (String)x.get(1)))
                .collect(Collectors.toList());
    }

    @Query(value = "select id, name from admin_project where user_id = :userId", nativeQuery = true)
    List<Tuple> listByAdmin(Long userId);

    default List<NamedCytomineDomain> listByAdmin(User user) {
        return listByAdmin(user.getId()).stream().map(x -> new NamedCytomineDomain(((Long)x.get(0)), (String)x.get(1)))
                .collect(Collectors.toList());
    }

    @Query(value = "select id, name from user_project where user_id = :userId", nativeQuery = true)
    List<Tuple> listByUser(Long userId);

    default List<NamedCytomineDomain> listByUser(User user) {
        return listByUser(user.getId()).stream().map(x -> new NamedCytomineDomain(((Long)x.get(0)), (String)x.get(1)))
                .collect(Collectors.toList());
    }


    @Query(value = "select id, created as date from project where id NOT IN (:ignoredProjectIds) order by date desc", nativeQuery = true)
    List<Tuple> listLastCreatedTuple(List<Long> ignoredProjectIds);

    @Query(value = "select id, created as date from project order by date desc", nativeQuery = true)
    List<Tuple> listLastCreatedTuple();

    default List<DatedCytomineDomain> listLastCreated(List<Long> ignoredProjectIds) {
        return listLastCreatedTuple(ignoredProjectIds).stream().map(x -> new DatedCytomineDomain(((Long)x.get(0)), (Date)x.get(1)))
                .collect(Collectors.toList());
    }

    default List<DatedCytomineDomain> listLastCreated() {
        return listLastCreatedTuple().stream().map(x -> new DatedCytomineDomain(((Long)x.get(0)), (Date)x.get(1)))
                .collect(Collectors.toList());
    }

    List<Project> findAllByOntology(Ontology ontology);

    List<Project> findAllByIdIn(List<Long> project);

    Optional<Project> findByName(String name);

    @Query(value = "SELECT a.project_id FROM image_instance a WHERE id=:imageInstanceId", nativeQuery = true)
    Long findByProjectIdByImageInstanceId(Long imageInstanceId);

}
