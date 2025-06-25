package be.cytomine.repositorynosql.social;

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

import be.cytomine.domain.social.PersistentProjectConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface PersistentProjectConnectionRepository extends MongoRepository<PersistentProjectConnection, Long> {

    Page<PersistentProjectConnection> findAllByUserAndProjectAndCreatedLessThan(Long user, Long project, Date created, PageRequest pageRequest);

    
    @Aggregation(pipeline = {"{$match: {project: ?0, user: ?1, $and : [{created: {$gte: ?3}},{created: {$lte: ?2}}]}},{$sort: {created: 1}},{$project: {dateInMillis: {$subtract: {'$created', ?4}}}}"})
    AggregationResults test(Long project, Long user, Date before, Date after, Date firstDate);

    Page<PersistentProjectConnection> findAllByUserAndProject(Long user, Long project, PageRequest pageRequest);

    @Aggregation(pipeline = {"{$match: {project: ?0}},{$sort: {?1: ?2}},{$group: {_id : '$user', created : {$max :'$created'}}}"})
    AggregationResults retrieve(Long project, String sortProperty, Integer sortDirection);

    Long countAllByProjectAndUser(Long project, Long user);

    @Aggregation(pipeline = {"{$group: {_id : '$project', total : {$sum :1}}}"})
    AggregationResults countConnectionByProject();

    Long countByProject(Long project);

    Long countByProjectAndCreatedAfter(Long project, Date createdMin);

    Long countByProjectAndCreatedBefore(Long project, Date createdMax);

    Long countByProjectAndCreatedBetween(Long project, Date createdMin, Date createdMax);
}
