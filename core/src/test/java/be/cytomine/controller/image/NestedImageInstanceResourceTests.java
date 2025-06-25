package be.cytomine.controller.image;

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
import be.cytomine.domain.image.NestedImageInstance;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.transaction.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class NestedImageInstanceResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restNestedImageInstanceControllerMockMvc;

    @Test
    @Transactional
    public void list_nested_image_instance_by_image_instance() throws Exception {
        NestedImageInstance nestedImageInstance = builder.given_a_nested_image_instance();

        restNestedImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{imageInstanceId}/nested.json", nestedImageInstance.getParent().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.collection[?(@.id=="+nestedImageInstance.getId()+")]").exists());

    }
    @Test
    @Transactional
    public void get_an_nested_image_instance() throws Exception {
        NestedImageInstance image = builder.given_a_nested_image_instance();

        restNestedImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{imageInstanceId}/nested/{id}.json", image.getParent().getId(), image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.image.NestedImageInstance"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.x").hasJsonPath())
                .andExpect(jsonPath("$.y").hasJsonPath())
                .andExpect(jsonPath("$.parent").hasJsonPath())
                .andExpect(jsonPath("$.baseImage").hasJsonPath()); // expect to have field from imageinstance
    }


    @Test
    @Transactional
    public void get_an_nested_image_instance_not_exist() throws Exception {
        restNestedImageInstanceControllerMockMvc.perform(get("/api/imageinstance/{imageInstanceId}/nested/{id}.json", 0, 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.message").exists());
    }


    @Test
    @Transactional
    public void add_valid_nested_image_instance() throws Exception {
        NestedImageInstance companionFile = builder.given_a_not_persisted_nested_image_instance();
        restNestedImageInstanceControllerMockMvc.perform(post("/api/imageinstance/{imageInstanceId}/nested.json", builder.given_an_image_instance().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companionFile.toJSON()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.nestedimageinstanceID").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.nestedimageinstance.id").exists());

    }

    @Test
    @Transactional
    public void edit_valid_nested_image_instance() throws Exception {
        NestedImageInstance nestedImageInstance = builder.given_a_nested_image_instance();
        JsonObject jsonObject = nestedImageInstance.toJsonObject();
        jsonObject.put("x", "123");
        restNestedImageInstanceControllerMockMvc.perform(put("/api/imageinstance/{imageInstanceId}/nested/{id}.json", nestedImageInstance.getParent().getId(), nestedImageInstance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.nestedimageinstanceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditNestedImageInstanceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.nestedimageinstance.id").exists())
                .andExpect(jsonPath("$.nestedimageinstance.x").value("123"));


    }


    @Test
    @Transactional
    public void delete_nested_image_instance() throws Exception {
        NestedImageInstance nestedImageInstance = builder.given_a_nested_image_instance();
        restNestedImageInstanceControllerMockMvc.perform(delete("/api/imageinstance/{imageInstanceId}/nested/{id}.json", nestedImageInstance.getParent().getId(), nestedImageInstance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.nestedimageinstanceID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteNestedImageInstanceCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.nestedimageinstance.id").exists());


    }

}
