package be.cytomine.service.meta;

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
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.GeometryUtils;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class PropertyServiceTests {

    @Autowired
    PropertyService propertyService;

    @Autowired
    BasicInstanceBuilder builder;

    @Test
    public void list_property() {
        Property property = builder.given_a_property(builder.given_a_project());
        assertThat(propertyService.list()).contains(property);
    }

    @Test
    public void list_property_for_domain() {
        Project project = builder.given_a_project();
        Property property = builder.given_a_property(project);
        assertThat(propertyService.list(project)).contains(property);
    }



    @Test
    public void find_by_id() {
        Property property = builder.given_a_property(builder.given_a_project());
        assertThat(propertyService.findById(property.getId())).isPresent();
    }

    @Test
    public void find_by_domain_and_key() {
        Project project = builder.given_a_project();
        Property property = builder.given_a_property(project);
        assertThat(propertyService.findByDomainAndKey(project, property.getKey())).isPresent();
    }


    @Test
    public void find_by_id_that_do_not_exists() {
        assertThat(propertyService.findById(0L)).isEmpty();
    }

    @Test
    public void create_property(){
        Project project = builder.given_a_project();
        CommandResponse commandResponse =
                propertyService.addProperty(project.getClass().getName(), project.getId(), "key", "value", builder.given_superadmin(), null);
        assertThat(commandResponse).isNotNull();
        assertThat(propertyService.findByDomainAndKey(project, "key")).isPresent();
    }

    @Test
    public void add_property() {
        Project project = builder.given_a_project();

        CommandResponse commandResponse =
                propertyService.add(builder.given_a_not_persisted_property(project, "key", "value").toJsonObject());
        assertThat(commandResponse).isNotNull();
        assertThat(propertyService.findByDomainAndKey(project, "key")).isPresent();
    }

    @Test
    void edit_valid_configuration_with_success() {
        Project project = builder.given_a_project();
        Property property = builder.given_a_property(project);

        assertThat(propertyService.findByDomainAndKey(project,"key")).isPresent();
        
        CommandResponse commandResponse = propertyService.update(property, property.toJsonObject().withChange("value", "NEW VALUE"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(propertyService.findByDomainAndKey(project,"key")).isPresent();
        Property edited = propertyService.findByDomainAndKey(project, "key").get();
        assertThat(edited.getValue()).isEqualTo("NEW VALUE");
    }


    @Test
    public void delete_property() {
        Property property = builder.given_a_property(builder.given_a_project());
        propertyService.delete(property, null, null, false);
        assertThat(propertyService.findById(property.getId())).isEmpty();
    }

    @Test
    public void list_keys_for_annotaion() {
        Project project = builder.given_a_project();
        UserAnnotation userAnnotation = builder.persistAndReturn(builder.given_a_not_persisted_user_annotation(project));
        Property property = builder.given_a_property(userAnnotation);
        Property projectProperty = builder.given_a_property(project, "projectKey", "value");

        List<Map<String, Object>> results = propertyService.listKeysForAnnotation(project, null, false);

        assertThat(results.stream().map(x -> (String)x.get("key"))).containsExactly(property.getKey()).doesNotContain(projectProperty.getKey());
    }

    @Test
    public void list_keys_for_annotation_by_image_with_user() {
        Project project = builder.given_a_project();
        UserAnnotation userAnnotation = builder.persistAndReturn(builder.given_a_not_persisted_user_annotation(project));
        Property property = builder.given_a_property(userAnnotation);
        Property projectProperty = builder.given_a_property(project, "projectKey", "value");

        List<Map<String, Object>> results = propertyService.listKeysForAnnotation(null, userAnnotation.getImage(), true);

        assertThat(results.stream().map(x -> (String)x.get("key"))).containsExactly(property.getKey()).doesNotContain(projectProperty.getKey());
        assertThat(results.stream().map(x -> (Long)x.get("user"))).containsExactly(builder.given_superadmin().getId());
    }

    @Test
    public void list_keys_for_image_instance() {
        Project project = builder.given_a_project();
        ImageInstance imageInstance = builder.given_an_image_instance(project);
        Property property = builder.given_a_property(imageInstance);
        Property projectProperty = builder.given_a_property(project, "projectKey", "value");

        List<String> results = propertyService.listKeysForImageInstance(imageInstance.getProject());

        assertThat(results).containsExactly(property.getKey()).doesNotContain(projectProperty.getKey());
    }

    @Test
    public void select_center_annotation() throws ParseException {
        Project project = builder.given_a_project();
        User user = builder.given_superadmin();
        ImageInstance imageInstance = builder.given_an_image_instance(project);
        UserAnnotation annotation = builder.given_a_user_annotation();
        annotation.setLocation(new WKTReader().read("POLYGON ((0 0, 0 1000, 1000 1000, 1000 0, 0 0))"));
        annotation.setImage(imageInstance);
        Property property = builder.persistAndReturn(builder.given_a_not_persisted_property(annotation, "TestCytomine", "ValueTestCytomine"));

        List<Map<String, Object>> results = propertyService.listAnnotationCenterPosition(user, imageInstance, GeometryUtils.createBoundingBox("0,0,1000,1000"), "TestCytomine");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("idAnnotation")).isEqualTo(annotation.getId());
    }
}
