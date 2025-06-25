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

import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.User;
import be.cytomine.dto.annotation.AnnotationIndexLightDTO;
import be.cytomine.repository.ontology.AnnotationIndexRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional
public class AnnotationIndexService {

    @Autowired
    private AnnotationIndexRepository annotationIndexRepository;

    @Autowired
    private EntityManager entityManager;

    public List<AnnotationIndexLightDTO> list(SliceInstance sliceInstance) {
        return annotationIndexRepository.findAllLightBySliceInstance(sliceInstance.getId());
    }

    /**
     * Return the number of annotation created by this user for this slice
     * If user is null, return the number of reviewed annotation for this slice
     */
    public Long count(SliceInstance slice, User user) {
        if (user!=null) {
            return annotationIndexRepository.findOneBySliceAndUser(slice, user)
                    .map(AnnotationIndexLightDTO::getCountAnnotation).orElse(0L);
        } else {
            return annotationIndexRepository.findAllBySlice(slice)
                    .stream().mapToLong(AnnotationIndexLightDTO::getCountReviewedAnnotation).sum();
        }
    }

    public Long count(List<SliceInstance> slices, User user) {
        if (user!=null) {
            return annotationIndexRepository.findOneBySliceInAndUser(slices, user)
                    .map(AnnotationIndexLightDTO::getCountAnnotation).orElse(0L);
        } else {
            return annotationIndexRepository.findAllBySliceIn(slices)
                    .stream().mapToLong(AnnotationIndexLightDTO::getCountReviewedAnnotation).sum();
        }
    }

}
