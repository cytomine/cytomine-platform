package be.cytomine.service.utils;

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
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.repository.AnnotationListing;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ParamServiceTests {

    @Autowired
    ParamsService paramsService;

    @Autowired
    BasicInstanceBuilder builder;

    @Test
    public void params_user(){
        Project project = builder.given_a_project();
        User userInProject = builder.given_a_user();
        builder.addUserToProject(project, userInProject.getUsername());
        User userNotInProject = builder.given_a_user();
        
        assertThat(paramsService.getParamsUserList(null, project))
                .contains(userInProject.getId()).doesNotContain(userNotInProject.getId());
        assertThat(paramsService.getParamsUserList("null", project))
                .contains(userInProject.getId()).doesNotContain(userNotInProject.getId());
        assertThat(paramsService.getParamsUserList(userInProject.getId()+"_"+userNotInProject.getId(), project))
                .contains(userInProject.getId()).doesNotContain(userNotInProject.getId());
        assertThat(paramsService.getParamsUserList(userNotInProject.getId()+"", project))
                .doesNotContain(userInProject.getId(), userNotInProject.getId());
    }

    @Test
    public void params_image_instance(){
        Project project = builder.given_a_project();
        ImageInstance imageInstanceInProject = builder.given_an_image_instance(project);
        ImageInstance imageInstanceNotInProject = builder.given_an_image_instance();
        
        assertThat(paramsService.getParamsImageInstanceList(null, project))
                .contains(imageInstanceInProject.getId()).doesNotContain(imageInstanceNotInProject.getId());
        assertThat(paramsService.getParamsImageInstanceList("null", project))
                .contains(imageInstanceInProject.getId()).doesNotContain(imageInstanceNotInProject.getId());
        assertThat(paramsService.getParamsImageInstanceList(imageInstanceInProject.getId()+"_"+imageInstanceNotInProject.getId(), project))
                .contains(imageInstanceInProject.getId()).doesNotContain(imageInstanceNotInProject.getId());
        assertThat(paramsService.getParamsImageInstanceList(imageInstanceNotInProject.getId()+"", project))
                .doesNotContain(imageInstanceInProject.getId(), imageInstanceNotInProject.getId());
    }


    @Test
    public void params_term(){
        
        Term termInProject = builder.given_a_term(builder.given_an_ontology());
        Term termNotInProject = builder.given_a_term();
        Project project = builder.given_a_project_with_ontology(termInProject.getOntology());
        
        assertThat(paramsService.getParamsTermList(null, project))
                .contains(termInProject.getId()).doesNotContain(termNotInProject.getId());
        assertThat(paramsService.getParamsTermList("null", project))
                .contains(termInProject.getId()).doesNotContain(termNotInProject.getId());
        assertThat(paramsService.getParamsTermList(termInProject.getId()+"_"+termNotInProject.getId(), project))
                .contains(termInProject.getId()).doesNotContain(termNotInProject.getId());
        assertThat(paramsService.getParamsTermList(termNotInProject.getId()+"", project))
                .doesNotContain(termInProject.getId(), termNotInProject.getId());
    }


    @Test
    public void property_group_to_show(){

        assertThat(paramsService.getPropertyGroupToShow(new JsonObject()))
                .containsExactlyElementsOf(AnnotationListing.availableColumnsDefault);

        assertThat(paramsService.getPropertyGroupToShow(JsonObject.of("showGIS", true)))
                .contains("gis");

        assertThat(paramsService.getPropertyGroupToShow(JsonObject.of("hideTerm", true)))
                .doesNotContain("term");
    }
}
