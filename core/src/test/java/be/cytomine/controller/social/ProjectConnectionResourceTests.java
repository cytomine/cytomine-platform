package be.cytomine.controller.social;

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
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.utils.JsonObject;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ProjectConnectionResourceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restProjectConnectionControllerMockMvc;

    @Autowired
    private PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    private LastConnectionRepository lastConnectionRepository;

    @Autowired
    private ProjectConnectionService projectConnectionService;

    @BeforeEach
    public void cleanDB() {
        persistentProjectConnectionRepository.deleteAll();
        lastConnectionRepository.deleteAll();
    }

    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project, "xxx", "linux", "chrome", "123");
        return connection;
    }

    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project, Date created) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project, "xxx", "linux", "chrome", "123", created);
        return connection;
    }

    @Test
    @Transactional
    public void add_connection() throws Exception {
        User user = builder.given_superadmin();
        Project project = builder.given_a_project();

        // {"project":"7577928","os":"Linux","browser":"chrome","browserVersion":"97.0.4692"}
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("project", project.getId());
        jsonObject.put("os", "Linux");
        jsonObject.put("browser", "chrome");
        jsonObject.put("browserVersion", "97.0.4692");
        
        restProjectConnectionControllerMockMvc.perform(post("/api/project/{id}/userconnection.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.social.PersistentProjectConnection"))
                .andExpect(jsonPath("$.user").value(user.getId()))
                .andExpect(jsonPath("$.project").value(project.getId()))
                .andExpect(jsonPath("$.browser").exists())
                .andExpect(jsonPath("$.browserVersion").exists());
        
    }

    @Test
    @Transactional
    public void get_connection_by_user_and_project() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/userconnection/{user}.json", project1.getId(), user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))));
    }

    @Test
    @Transactional
    public void get_connection_by_project() throws Exception {
        User user = builder.given_a_user();
        User anotherUser = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(anotherUser, project1, DateUtils.addSeconds(new Date(), -2));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -1));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/lastConnection.json", project1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))));
    }

    @Test
    @Transactional
    public void get_last_connection_by_project() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/lastConnection.json", project1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].user").value(user.getId()));
    }

    @Test
    @Transactional
    public void get_last_connection_by_project_by_user() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/lastConnection/{user}.json", project1.getId(), user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].user").value(user.getId()));
    }

    @Test
    @Transactional
    public void get_number_connection_by_project() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency.json", project1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].frequency").value(2));
    }

    @Test
    @Transactional
    public void get_number_connection_by_project_heatmap() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency.json", project1.getId())
                        .param("heatmap", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void get_number_connection_by_project_with_period() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency.json", project1.getId())
                        .param("period", "week"))
                .andExpect(status().isOk());
    }


    @Test
    @Transactional
    public void get_number_connection_frequency_by_user() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency/{user}.json", project1.getId(), user.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void get_number_connection_frequency_by_user_with_heatmap() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency/{user}.json", project1.getId(), user.getId())
                        .param("heatmap", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void get_number_connection_frequency_by_user_with_period() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency/{user}.json", project1.getId(), user.getId())
                        .param("period", "week"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void get_connection_frequency() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/connectionFrequency.json")
                        .param("period", "week"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void average_econnection() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/averageConnections.json")
                .param("period", "week"))
                .andExpect(status().isOk());

    }

    @Test
    @Transactional
    public void user_connection_count() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/userconnection/count.json", project1.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void connection_history() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionHistory/{user}.json", project1.getId(), user.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void connection_history_with_export_csv() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        Date firstDate = DateUtils.addSeconds(new Date(), -3);
        Date secondDate = DateUtils.addSeconds(new Date(), -2);
        given_a_persistent_connection_in_project(user, project1, firstDate);
        given_a_persistent_connection_in_project(user, project1, secondDate);

        MvcResult mvcResult = restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionHistory/{user}.json", project1.getId(), user.getId())
                        .param("export", "csv"))
                .andExpect(status().isOk()).andReturn();

        String[] rows = mvcResult.getResponse().getContentAsString().split("\n");
        String[] firstConnection = rows[1].split(";");
        checkConnectionHistoryResult(firstConnection, secondDate, "0", "0", "0", "linux", "chrome", "123");
        String[] secondConnection = rows[2].split(";");
        checkConnectionHistoryResult(secondConnection, firstDate, "0", "0", "0", "linux", "chrome", "123");
    }

    private void checkConnectionHistoryResult(String[] result, Date date, String time, String countViewedImages, String countCreatedAnnotations, String os, String browser, String browserVersion){
        AssertionsForClassTypes.assertThat(result[0]).isEqualTo(be.cytomine.utils.DateUtils.computeMillisInDate(date.getTime()).toString());
        AssertionsForClassTypes.assertThat(result[1]).isEqualTo(time);
        AssertionsForClassTypes.assertThat(result[2]).isEqualTo(countViewedImages);
        AssertionsForClassTypes.assertThat(result[3]).isEqualTo(countCreatedAnnotations);
        AssertionsForClassTypes.assertThat(result[4]).isEqualTo(os);
        AssertionsForClassTypes.assertThat(result[5]).isEqualTo(browser);
        AssertionsForClassTypes.assertThat(result[6].replace("\r", "")).isEqualTo(browserVersion);
    }

    @Test
    @Transactional
    public void activity_detail() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        PersistentProjectConnection connection = given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));

        restProjectConnectionControllerMockMvc.perform(get("/api/projectConnection/{id}.json", connection.getId()))
                .andExpect(status().isOk());

    }

    @Disabled("Disabled until ReportService is up!")
    @Test
    @Transactional
    public void activity_detail_with_export_csv() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        PersistentProjectConnection connection = given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));

        restProjectConnectionControllerMockMvc.perform(get("/api/projectConnection/{id}.json", connection.getId())
                        .param("export", "csv"))
                .andExpect(status().isOk());
    }

}
