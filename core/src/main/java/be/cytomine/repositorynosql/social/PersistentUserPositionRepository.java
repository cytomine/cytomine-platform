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

import be.cytomine.domain.social.PersistentImageConsultation;
import be.cytomine.domain.social.PersistentUserPosition;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


@Repository
public interface PersistentUserPositionRepository extends MongoRepository<PersistentUserPosition, Long> {


    @Aggregation(pipeline = {"{$match: {project: ?0, user: ?1, image: ?2, $and : [{created: {$gte: ?4}},{created: {$lte: ?3}}]}},{$sort: {created: 1}},{$project: {dateInMillis: {$subtract: {'$created', ?5}}}}"})
    AggregationResults retrieve(Long project, Long user, Long image, Date before, Date after, Date firstDate);

    void deleteAllByImage(Long id);
}
