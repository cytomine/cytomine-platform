package be.cytomine.service.ontology;

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
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.repository.ontology.ReviewedAnnotationRepository;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.security.SecurityACLService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@Slf4j
@Service
@Transactional
public class GenericAnnotationService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AnnotationDomainRepository annotationDomainRepository;

    @Autowired
    private ReviewedAnnotationRepository reviewedAnnotationRepository;

    @Autowired
    private UserAnnotationRepository userAnnotationRepository;

    /**
     * Find all annotation id from a specific table created by a user that touch location geometry
     * @param location WKT Location that must touch result annotation
     * @param idImage Annotation image
     * @param layers Annotation Users
     * @param table Table that store annotation (user, algo, reviewed)
     * @return List of annotation id from idImage and idUser that touch location
     */
    public List<AnnotationDomain> findAnnotationThatTouch(String location, List<Long> layers, long idImage, String table) {
        ImageInstance image = entityManager.find(ImageInstance.class, idImage);

        boolean projectAdmin = securityACLService.hasPermission(image.getProject(), ADMINISTRATION);
        if(!projectAdmin) {
            layers = layers.stream().filter(x -> Objects.equals(x, currentUserService.getCurrentUser().getId()))
                    .collect(Collectors.toList());
        }

        List<Tuple> results;
        if (table.equals("reviewed_annotation")) {
            results = annotationDomainRepository.findAllIntersectForReviewedAnnotations(image.getId(), location);
        } else {
            results = annotationDomainRepository.findAllIntersectForUserAnnotations(image.getId(), layers, location);
        }

        List<Long> ids = new ArrayList<>();
        Set<Long> users = new HashSet<>();
        for (Tuple result : results) {
            ids.add((Long)result.get("annotation"));
            users.add((Long)result.get("user"));
        }

        if(users.size()>1 && !table.equals("reviewed_annotation")) { //if more annotation from more than 1 user NOT IN REVIEW MODE!
            throw new WrongArgumentException("Annotations from multiple users are under this area. You can correct only annotation from 1 user (hide layer if necessary)");
        }

        List<AnnotationDomain> annotations = annotationDomainRepository.findAllById(ids);

        Map<Long, Double> termSizes = new HashMap<>();
        for (AnnotationDomain annotation : annotations) {
            for (Long termId : annotation.termsId()) {
                Double currentValue = termSizes.getOrDefault(termId, 0d);
                termSizes.put(termId, currentValue + annotation.getArea());
            }
        }

        Double min = Double.MAX_VALUE;
        Long goodTerm = null;



        if(!termSizes.isEmpty()) {
            for (Map.Entry<Long, Double> entry : termSizes.entrySet()) {
                if (min > entry.getValue()) {
                    min = entry.getValue();
                    goodTerm = entry.getKey();
                }
            }

//            annotations = new ArrayList<>();
            for (AnnotationDomain annotation : annotations) {
                if (!annotation.termsId().contains(goodTerm)) {
                    annotations.remove(annotation);
                }
            }
        }

        return annotations.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Find all reviewed annotation domain instance with ids and exactly the same term
     * All these annotation must have this single term
     * @param ids List of reviewed annotation id
     * @param termsId Term that must have all reviewed annotation (
     * @return Reviewed Annotation list
     */
    public List<ReviewedAnnotation> findReviewedAnnotationWithTerm(List<Long> ids, List<Long> termsId) {
        List<ReviewedAnnotation> annotationsWithSameTerm = new ArrayList<>();
        for (Long id : ids) {
            ReviewedAnnotation compared = reviewedAnnotationRepository.findById(id).get();
            List<Long> idTerms = compared.termsId();
            if (idTerms.size() != termsId.size()) {
                throw new WrongArgumentException("Annotations have not the same term!");
            }

            for (Long idTerm : idTerms) {
                if (!termsId.contains(idTerm)) {
                    throw new WrongArgumentException("Annotations have not the same term!");
                }
            }
            annotationsWithSameTerm.add(compared);
        }
        return annotationsWithSameTerm;
    }

    /**
     * Find all user annotation domain instance with ids and exactly the same term
     * All these annotation must have this single term
     * @param ids List of user annotation id
     * @param termsId Term that must have all user annotation (
     * @return user Annotation list
     */
    public List<UserAnnotation> findUserAnnotationWithTerm(List<Long> ids, List<Long> termsId) {
        List<UserAnnotation> annotationsWithSameTerm = new ArrayList<>();
        for (Long id : ids) {
            UserAnnotation compared = userAnnotationRepository.findById(id).get();
            List<Long> idTerms = compared.termsId();
            if (idTerms.size() != termsId.size()) {
                throw new WrongArgumentException("Annotations have not the same term!");
            }

            for (Long idTerm : idTerms) {
                if (!termsId.contains(idTerm)) {
                    throw new WrongArgumentException("Annotations have not the same term!");
                }
            }
            annotationsWithSameTerm.add(compared);
        }
        return annotationsWithSameTerm;
    }


}
