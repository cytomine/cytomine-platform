package be.cytomine.controller.ontology;

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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationIndex;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.annotation.AnnotationIndexLightDTO;
import be.cytomine.repository.ontology.AnnotationIndexRepository;

import org.locationtech.jts.io.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class AnnotationIndexResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restAnnotationIndexControllerMockMvc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AnnotationIndexRepository annotationIndexRepository;

    Project project;
    ImageInstance image;
    SliceInstance slice;
    User me;
    Term term;
    UserAnnotation a1;
    UserAnnotation a2;
    UserAnnotation a3;
    UserAnnotation a4;

    void createAnnotationSet() throws ParseException {
        project = builder.given_a_project();
        image = builder.given_an_image_instance(project);
        slice = builder.given_a_slice_instance(image,0,0,0);
        me = builder.given_superadmin();
        term =  builder.given_a_term(project.getOntology());

        a1 =  builder.given_a_user_annotation(slice,"POLYGON((1 1,5 1,5 5,1 5,1 1))", me, term);
        a2 =  builder.given_a_user_annotation(slice,"POLYGON((1 1,5 1,5 5,1 5,1 1))", me, term);
        a3 =  builder.given_a_user_annotation(slice,"POLYGON((1 1,5 1,5 5,1 5,1 1))", me, term);

        a4 =  builder.given_a_user_annotation(slice,"POLYGON((1 1,5 1,5 5,1 5,1 1))", me, null);
        em.flush();
    }

    @BeforeEach
    public void BeforeEach() throws ParseException {
        this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    createAnnotationSet();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Test
    public void list_user_annotation_property_show() throws Exception {

        List<AnnotationIndex> all = annotationIndexRepository.findAll();
        List<AnnotationIndexLightDTO> slices = annotationIndexRepository.findAllBySlice(slice);
        List<AnnotationIndexLightDTO> slicesLight = annotationIndexRepository.findAllLightBySliceInstance(slice.getId());
        restAnnotationIndexControllerMockMvc.perform(get("/api/sliceinstance/{id}/annotationindex.json", slice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.user==" + me.getId() + ")]").exists())
                .andExpect(jsonPath("$.collection[?(@.user==" + me.getId() + ")].slice").value(slice.getId().intValue()))
                .andExpect(jsonPath("$.collection[?(@.user==" + me.getId() + ")].countAnnotation").value(4))
                .andExpect(jsonPath("$.collection[?(@.user==" + me.getId() + ")].countReviewedAnnotation").value(0));
    }
}
