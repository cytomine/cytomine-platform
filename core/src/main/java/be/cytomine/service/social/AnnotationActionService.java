package be.cytomine.service.social;

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
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.AnnotationAction;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.repositorynosql.social.*;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.security.SecurityACLService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.*;

import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class AnnotationActionService {

    public static final String DATABASE_NAME = "cytomine";
    @Autowired
    CurrentUserService currentUserService;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    AnnotationDomainRepository annotationDomainRepository;

    @Autowired
    AnnotationActionRepository annotationActionRepository;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    MongoTemplate mongoTemplate;

    public AnnotationAction add(AnnotationDomain annotation, User user, String action, Date created) {
        securityACLService.check(annotation,READ);
        AnnotationAction annotationAction = new AnnotationAction();
        annotationAction.setId(sequenceService.generateID());
        annotationAction.setUser(user.getId());
        annotationAction.setImage(annotation.getImage().getId());
        annotationAction.setSlice(annotation.getSlice().getId());
        annotationAction.setProject(annotation.getProject().getId());
        annotationAction.setCreated(created);
        annotationAction.setAction(action);
        annotationAction.setAnnotationIdent(annotation.getId());
        annotationAction.setAnnotationClassName(annotation.getClass().getName());
        annotationAction.setAnnotationCreator(annotation.user().getId());

        return annotationActionRepository.insert(annotationAction);
    }

    public List<AnnotationAction> list(SliceInstance sliceInstance, User user, Long afterThan, Long beforeThan) {
        securityACLService.checkIsAdminContainer(sliceInstance);
        Query query = new Query();
        query.addCriteria(Criteria.where("slice").is(sliceInstance.getId()));
        if (user!=null) {
            query.addCriteria(Criteria.where("user").is(user.getId()));
        }
        if (afterThan!=null && beforeThan!=null) {
            query.addCriteria(Criteria.where("created").gte(new Date(afterThan)).lte(new Date(beforeThan)));
        } else if (afterThan!=null) {
            query.addCriteria(Criteria.where("created").gte(new Date(afterThan)));
        } else if (beforeThan!=null) {
            query.addCriteria(Criteria.where("created").lte(new Date(beforeThan)));
        }
        query.with(Sort.by(Sort.Direction.ASC, "created"));

        return mongoTemplate.find(query, AnnotationAction.class);
    }


    public List<AnnotationAction> list(ImageInstance imageInstance, User user, Long afterThan, Long beforeThan) {
        securityACLService.checkIsAdminContainer(imageInstance);
        Query query = new Query();
        query.addCriteria(Criteria.where("image").is(imageInstance.getId()));
        if (user!=null) {
            query.addCriteria(Criteria.where("user").is(user.getId()));
        }
        if (afterThan!=null && beforeThan!=null) {
            query.addCriteria(Criteria.where("created").gte(new Date(afterThan)).lte(new Date(beforeThan)));
        } else if (afterThan!=null) {
            query.addCriteria(Criteria.where("created").gte(new Date(afterThan)));
        } else if (beforeThan!=null) {
            query.addCriteria(Criteria.where("created").lte(new Date(beforeThan)));
        }
        query.with(Sort.by(Sort.Direction.ASC, "created"));

        return mongoTemplate.find(query, AnnotationAction.class);
    }

    public Long countByProject(Project project, Long startDate, Long endDate) {
        if (startDate==null && endDate==null) {
            return annotationActionRepository.countByProject(project.getId());
        } else if (endDate==null) {
            return annotationActionRepository.countByProjectAndCreatedAfter(project.getId(), new Date(startDate));
        } else if (startDate==null) {
            return annotationActionRepository.countByProjectAndCreatedBefore(project.getId(), new Date(endDate));
        } else {
            return annotationActionRepository.countByProjectAndCreatedBetween(project.getId(), new Date(startDate), new Date(endDate));
        }
    }
}
