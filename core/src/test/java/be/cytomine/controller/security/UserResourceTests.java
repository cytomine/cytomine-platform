package be.cytomine.controller.security;

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
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.*;
import be.cytomine.dto.image.AreaDTO;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.repositorynosql.social.*;
import be.cytomine.service.PermissionService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.social.ImageConsultationService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.service.social.UserPositionService;
import be.cytomine.service.social.UserPositionServiceTests;
import be.cytomine.utils.JsonObject;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
import org.springframework.web.context.request.RequestContextHolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
public class UserResourceTests {

    @Autowired
    private PersistentConnectionRepository persistentConnectionRepository;

    @Autowired
    private LastConnectionRepository lastConnectionRepository;

    @Autowired
    private PersistentImageConsultationRepository persistentImageConsultationRepository;

    @Autowired
    private PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    private ProjectConnectionRepository projectConnectionRepository;

    @Autowired
    private PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    private LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private UserPositionService userPositionService;

    @Autowired
    private ImageConsultationService imageConsultationService;

    @Autowired
    private ProjectConnectionService projectConnectionService;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restUserControllerMockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    public void init() {
        persistentConnectionRepository.deleteAll();
        lastConnectionRepository.deleteAll();
        persistentImageConsultationRepository.deleteAll();
        persistentProjectConnectionRepository.deleteAll();
        projectConnectionRepository.deleteAll();
        lastUserPositionRepository.deleteAll();
        persistentUserPositionRepository.deleteAll();
    }

    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project, Date created) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project, "xxx", "linux", "chrome", "123", created);
        return connection;
    }

    PersistentImageConsultation given_a_persistent_image_consultation(User user, ImageInstance imageInstance, Date created) {
        return imageConsultationService.add(user, imageInstance.getId(), "xxx", "mode", created);
    }


    PersistentUserPosition given_a_persistent_user_position(Date creation, User user, SliceInstance sliceInstance, AreaDTO areaDTO) {
        PersistentUserPosition connection =
                userPositionService.add(creation, user, sliceInstance, sliceInstance.getImage(), areaDTO,
                        1,
                        5.0,
                        false
                );
        return connection;
    }

    PersistentConnection given_a_last_connection(User user, Long idProject, Date date) {
        LastConnection connection = new LastConnection();
        connection.setId(sequenceService.generateID());
        connection.setUser(user.getId());
        connection.setDate(date);
        connection.setCreated(date);
        connection.setProject(idProject);
        lastConnectionRepository.insert(connection); //don't use save (stateless collection)

        PersistentConnection connectionPersist = new PersistentConnection();
        connectionPersist.setId(sequenceService.generateID());
        connectionPersist.setUser(user.getId());
        connectionPersist.setCreated(date);
        connectionPersist.setProject(idProject);
        connectionPersist.setSession(RequestContextHolder.currentRequestAttributes().getSessionId());
        persistentConnectionRepository.insert(connectionPersist); //don't use save (stateless collection)
        return connectionPersist;
    }

    @Test
    @Transactional
    public void list_project_admin() throws Exception {
        User projectAdmin = builder.given_a_user();
        User projectUser = builder.given_a_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, projectAdmin.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);

        restUserControllerMockMvc.perform(get("/api/project/{id}/admin.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectAdmin.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").doesNotExist());

    }

    @Test
    @Transactional
    public void list_project_admin_as_non_admin_user() throws Exception {
        User projectAdmin = builder.given_a_user();
        User projectUser = builder.given_a_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, projectAdmin.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);

        restUserControllerMockMvc.perform(get("/api/project/{id}/admin.json", project.getId()).with(user(projectAdmin.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectAdmin.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").doesNotExist());

    }


    @Test
    @Transactional
    public void list_project_representatives() throws Exception {
        User projectPrepresentative = builder.given_a_user();
        User projectUser = builder.given_a_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, projectPrepresentative.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);
        builder.given_a_project_representative_user(project, projectPrepresentative);

        restUserControllerMockMvc.perform(get("/api/project/{id}/users/representative.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectPrepresentative.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").doesNotExist());
    }

    @Test
    @Transactional
    public void list_project_creator() throws Exception {
        User projectCreator = builder.given_superadmin();
        User projectUser = builder.given_a_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, projectCreator.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);
        builder.given_a_project_representative_user(project, projectCreator);

        restUserControllerMockMvc.perform(get("/api/project/{id}/users/representative.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectCreator.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_ontology_user() throws Exception {
        User projectAdmin = builder.given_a_user();
        User projectUser = builder.given_a_user();
        User simpleUser = builder.given_a_user();
        Ontology ontology = builder.given_an_ontology();
        Project project = builder.given_a_project_with_ontology(ontology);
        builder.addUserToProject(project, projectAdmin.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);

        restUserControllerMockMvc.perform(get("/api/ontology/{id}/user.json", ontology.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectAdmin.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + simpleUser.getUsername() + "')]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_storage_user() throws Exception {
        User storageAdmin = builder.given_a_user();
        User storageUser = builder.given_a_user();
        User simpleUser = builder.given_a_user();
        Storage storage = builder.given_a_storage();
        builder.addUserToStorage(storage, storageAdmin.getUsername(), ADMINISTRATION);
        builder.addUserToStorage(storage, storageUser.getUsername(), READ);

        restUserControllerMockMvc.perform(get("/api/storage/{id}/user.json", storage.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.username=='" + storageAdmin.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + storageUser.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + simpleUser.getUsername() + "')]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_project_userlayer() throws Exception {
        User projectAdmin = builder.given_a_user();
        User projectUser = builder.given_a_user();
        User simpleUser = builder.given_a_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, projectAdmin.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);

        restUserControllerMockMvc.perform(get("/api/project/{id}/userlayer.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectAdmin.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + simpleUser.getUsername() + "')]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_user() throws Exception {
        User projectAdmin = builder.given_a_user();
        User projectUser = builder.given_a_user();
        User simpleUser = builder.given_a_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, projectAdmin.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);

        restUserControllerMockMvc.perform(get("/api/user.json")
                        .param("sortColumn", "created")
                        .param("sortDirection", "desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.username=='" + simpleUser.getUsername() + "')].name").value("firstname lastname"))
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')].name").value("firstname lastname"))
                .andExpect(jsonPath("$.collection[?(@.username=='" + simpleUser.getUsername() + "')].name").value("firstname lastname"));
    }

    @Test
    @Transactional
    public void get_user_as_superadmin() throws Exception {
        User currentUser = builder.given_superadmin();

        restUserControllerMockMvc.perform(get("/api/user/{id}.json", builder.given_superadmin().getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(currentUser.getId()))
                .andExpect(jsonPath("$.username").value(currentUser.getUsername()))
                .andExpect(jsonPath("$.name").value(currentUser.getName()))
                .andExpect(jsonPath("$.fullName").value(currentUser.getFullName()))
                .andExpect(jsonPath("$.firstname").doesNotExist())
                .andExpect(jsonPath("$.created").doesNotExist())
                .andExpect(jsonPath("$.origin").doesNotExist())
                .andExpect(jsonPath("$.admin").doesNotExist())
                .andExpect(jsonPath("$.language").doesNotExist())
                .andExpect(jsonPath("$.publicKey").doesNotExist())
                .andExpect(jsonPath("$.lastname").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.guest").doesNotExist())
                .andExpect(jsonPath("$.passwordExpired").doesNotExist())
                .andExpect(jsonPath("$.updated").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.algo").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @Transactional
    @WithMockUser(username = "user")
    public void get_user_as_current_user() throws Exception {
        User currentUser = builder.given_default_user();


        restUserControllerMockMvc.perform(get("/api/user/{id}.json", currentUser.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(currentUser.getId()))
                .andExpect(jsonPath("$.username").value(currentUser.getUsername()))
                .andExpect(jsonPath("$.name").value(currentUser.getName()))
                .andExpect(jsonPath("$.fullName").value(currentUser.getFullName()))
                .andExpect(jsonPath("$.firstname").doesNotExist())
                .andExpect(jsonPath("$.created").doesNotExist())
                .andExpect(jsonPath("$.origin").doesNotExist())
                .andExpect(jsonPath("$.admin").doesNotExist())
                .andExpect(jsonPath("$.language").doesNotExist())
                .andExpect(jsonPath("$.publicKey").doesNotExist())
                .andExpect(jsonPath("$.lastname").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.guest").doesNotExist())
                .andExpect(jsonPath("$.passwordExpired").doesNotExist())
                .andExpect(jsonPath("$.updated").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.algo").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @Transactional
    @WithMockUser(username = "user")
    public void get_user_as_another_user() throws Exception {
        User currentUser = builder.given_a_user();

        restUserControllerMockMvc.perform(get("/api/user/{id}.json", builder.given_superadmin().getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(builder.given_superadmin().getId()))
                .andExpect(jsonPath("$.username").value(builder.given_superadmin().getUsername()))
                .andExpect(jsonPath("$.name").value(builder.given_superadmin().getName()))
                .andExpect(jsonPath("$.fullName").value(builder.given_superadmin().getFullName()))
                .andExpect(jsonPath("$.firstname").doesNotExist())
                .andExpect(jsonPath("$.created").doesNotExist())
                .andExpect(jsonPath("$.origin").doesNotExist())
                .andExpect(jsonPath("$.admin").doesNotExist())
                .andExpect(jsonPath("$.language").doesNotExist())
                .andExpect(jsonPath("$.publicKey").doesNotExist())
                .andExpect(jsonPath("$.lastname").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.guest").doesNotExist())
                .andExpect(jsonPath("$.passwordExpired").doesNotExist())
                .andExpect(jsonPath("$.updated").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.algo").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @Transactional
    public void get_user_with_its_username() throws Exception {
        User currentUser = builder.given_superadmin();
        restUserControllerMockMvc.perform(get("/api/user/{id}.json", builder.given_superadmin().getUsername())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(currentUser.getUsername()));
    }

    @Test
    @Transactional
    public void get_user_with_its_user_key() throws Exception {
        User currentUser = builder.given_superadmin();
        restUserControllerMockMvc.perform(get("/api/userkey/{publicKey}/keys.json", builder.given_superadmin().getPublicKey())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.privateKey").value(currentUser.getPrivateKey()));
    }

    @Test
    @Transactional
    public void get_user_with_its_user_key_id() throws Exception {
        User currentUser = builder.given_superadmin();
        restUserControllerMockMvc.perform(get("/api/user/{id}/keys.json", builder.given_superadmin().getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.privateKey").value(currentUser.getPrivateKey()));
    }

    @Test
    @Transactional
    @WithMockUser(username = "user")
    public void get_keys_from_other_user_is_forbidden() throws Exception {
        User user = builder.given_superadmin();
        restUserControllerMockMvc.perform(get("/api/user/{id}/keys.json", user.getId()))
                .andExpect(status().isForbidden());
        restUserControllerMockMvc.perform(get("/api/userkey/{publicKey}/keys.json", user.getPublicKey()))
                .andExpect(status().isForbidden());
    }


    @Test
    @Transactional
    @WithMockUser(username = "user")
    public void get_signature() throws Exception {
        User user = builder.given_default_user();
        restUserControllerMockMvc.perform(get("/api/signature.json").param("method", "GET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value(user.getPublicKey()))
                .andExpect(jsonPath("$.signature").isNotEmpty());

    }

    @Test
    @Transactional
    public void get_current_user_keys() throws Exception {
        User currentUser = builder.given_superadmin();

        restUserControllerMockMvc.perform(get("/api/user/current/keys")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryKey").value(currentUser.getPublicKey()))
                .andExpect(jsonPath("$.secondaryKey").value(currentUser.getPrivateKey()));
    }

    @Test
    @Transactional
    public void regenerate_current_user_keys() throws Exception {
        User currentUser = builder.given_superadmin();

        String oldPrimaryKey = currentUser.getPublicKey();
        String oldSecondaryKey = currentUser.getPrivateKey();

        restUserControllerMockMvc.perform(post("/api/user/current/keys")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryKey").value(not(oldPrimaryKey)))
                .andExpect(jsonPath("$.secondaryKey").value(not(oldSecondaryKey)));
    }


    @Test
    @Transactional
    public void get_current_user() throws Exception {
        User currentUser = builder.given_superadmin();

        restUserControllerMockMvc.perform(get("/api/user/current.json")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(currentUser.getId()))
                .andExpect(jsonPath("$.username").value(currentUser.getUsername()))
                .andExpect(jsonPath("$.name").value(currentUser.getName()))
                .andExpect(jsonPath("$.fullName").value(currentUser.getFullName()))
                .andExpect(jsonPath("$.firstname").doesNotExist())
                .andExpect(jsonPath("$.created").doesNotExist())
                .andExpect(jsonPath("$.origin").doesNotExist())
                .andExpect(jsonPath("$.admin").doesNotExist())
                .andExpect(jsonPath("$.language").doesNotExist())
                .andExpect(jsonPath("$.publicKey").doesNotExist())
                .andExpect(jsonPath("$.lastname").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.guest").doesNotExist())
                .andExpect(jsonPath("$.passwordExpired").doesNotExist())
                .andExpect(jsonPath("$.updated").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.algo").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }


//    @Test
//    @Transactional
//    public void add_valid_user() throws Exception {
//
//        restUserControllerMockMvc.perform(post("/api/user.json")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"name\":\"TEST_CREATE\",\"reference\":\""+ UUID.randomUUID().toString() +"\",\"firstname\":\"TEST_CREATE\",\"lastname\":\"TEST_CREATE\",\"username\":\"TEST_CREATE\",\"email\":\"loicrollus@gmail.com\",\"language\":\"EN\",\"password\":\"TEST_CREATE\"}"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.printMessage").value(true))
//                .andExpect(jsonPath("$.callback").exists())
//                .andExpect(jsonPath("$.callback.userID").exists())
//                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddUserCommand"))
//                .andExpect(jsonPath("$.message").exists())
//                .andExpect(jsonPath("$.command").exists())
//                .andExpect(jsonPath("$.user.id").exists())
//                .andExpect(jsonPath("$.user.username").value("TEST_CREATE"));
//
//        User user = userRepository.findByUsernameLikeIgnoreCase("TEST_CREATE").get();
//
//    }
//
//
//    @Test
//    @Transactional
//    public void add_user_refused_if_username_exists() throws Exception {
//        User user = builder.given_a_user();
//        restUserControllerMockMvc.perform(post("/api/user.json")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(user.toJSON()))
//                .andDo(print())
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    @Transactional
//    public void add_user_refused_if_username_not_set() throws Exception {
//        User user = builder.given_a_user();
//        restUserControllerMockMvc.perform(post("/api/user.json")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"firstname\":\"TEST_CREATE\",\"lastname\":\"TEST_CREATE\",\"email\":\"loicrollus@gmail.com\",\"language\":\"EN\",\"password\":\"TEST_CREATE\"}"))
//                .andDo(print())
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false));
//    }
//
//    @Test
//    @Transactional
//    public void edit_valid_user() throws Exception {
//
//        restUserControllerMockMvc.perform(post("/api/user.json")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"name\":\"TEST_CREATE\",\"reference\":\""+ UUID.randomUUID().toString() +"\",\"firstname\":\"TEST_CREATE\",\"lastname\":\"TEST_CREATE\",\"username\":\"TEST_CREATE\",\"email\":\"loicrollus@gmail.com\",\"language\":\"EN\",\"password\":\"TEST_CREATE\"}"))
//                .andDo(print())
//                .andExpect(status().isOk());
//
//
//        User user = userRepository.findByUsernameLikeIgnoreCase("TEST_CREATE").get();
//
//        JsonObject jsonObject = user.toJsonObject();
//        jsonObject.put("name", "TEST_CREATE_CHANGE");
//
//        restUserControllerMockMvc.perform(put("/api/user/{id}.json", jsonObject.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(jsonObject.toJsonString()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.printMessage").value(true))
//                .andExpect(jsonPath("$.callback").exists())
//                .andExpect(jsonPath("$.message").exists())
//                .andExpect(jsonPath("$.command").exists())
//                .andExpect(jsonPath("$.user.id").exists())
//                .andExpect(jsonPath("$.user.name").value("TEST_CREATE_CHANGE"));
//    }
//
//    @Test
//    @Transactional
//    public void delete_user() throws Exception {
//        User user = builder.given_a_user();
//        restUserControllerMockMvc.perform(delete("/api/user/{id}.json", user.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(user.toJSON()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.printMessage").value(true))
//                .andExpect(jsonPath("$.callback").exists())
//                .andExpect(jsonPath("$.callback.userID").exists())
//                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteUserCommand"))
//                .andExpect(jsonPath("$.message").exists())
//                .andExpect(jsonPath("$.command").exists())
//                .andExpect(jsonPath("$.user.id").exists());
//    }


    @Test
    @Transactional
    public void list_project_users_with_role_filter() throws Exception {
        User projectAdmin = builder.given_a_user();
        User projectUser = builder.given_a_user();
        User projectPrepresentative = builder.given_a_user();
        User simpleUser = builder.given_a_user();
        Project project = builder.given_a_project();

        builder.addUserToProject(project, projectPrepresentative.getUsername(), ADMINISTRATION);
        builder.given_a_project_representative_user(project, projectPrepresentative);
        builder.addUserToProject(project, projectAdmin.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);

        restUserControllerMockMvc.perform(get("/api/project/{id}/user.json", project.getId())
                        .param("max", "25")
                        .param("offset", "0")
                        .param("projectRole[in]", "contributor,manager,representative")
                        .param("sort", "")
                        .param("order", "")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectPrepresentative.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectAdmin.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + simpleUser.getUsername() + "')]").doesNotExist());

        restUserControllerMockMvc.perform(get("/api/project/{id}/user.json", project.getId())
                        .param("max", "25")
                        .param("offset", "0")
                        .param("projectRole[in]", "representative")
                        .param("sort", "")
                        .param("order", "")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectPrepresentative.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectAdmin.getUsername() + "')]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.username=='" + simpleUser.getUsername() + "')]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_project_users_with_pagination() throws Exception {
        User projectPrepresentative = builder.given_a_user();
        User projectAdmin = builder.given_a_user();
        User projectUser = builder.given_a_user();
        User simpleUser = builder.given_a_user();
        Project project = builder.given_a_project();

        builder.addUserToProject(project, projectPrepresentative.getUsername(), ADMINISTRATION);
        builder.given_a_project_representative_user(project, projectPrepresentative);
        builder.addUserToProject(project, projectAdmin.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);

        restUserControllerMockMvc.perform(get("/api/project/{id}/user.json", project.getId())
                        .param("max", "2")
                        .param("offset", "0")
                        .param("projectRole[in]", "contributor,manager,representative")
                        .param("sortColumn", "created")
                        .param("sortDirection", "asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(2)))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.perPage").value(2))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.collection[0].username").value(projectPrepresentative.getUsername()))
                .andExpect(jsonPath("$.collection[1].username").value(projectAdmin.getUsername()));

        restUserControllerMockMvc.perform(get("/api/project/{id}/user.json", project.getId())
                        .param("max", "2")
                        .param("offset", "2")
                        .param("projectRole[in]", "contributor,manager,representative")
                        .param("sortColumn", "created")
                        .param("sortDirection", "asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.offset").value(2))
                .andExpect(jsonPath("$.perPage").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.collection[0].username").value(projectUser.getUsername()));

        restUserControllerMockMvc.perform(get("/api/project/{id}/user.json", project.getId())
                        .param("max", "2")
                        .param("offset", "4")
                        .param("projectRole[in]", "contributor,manager,representative")
                        .param("sortColumn", "created")
                        .param("sortDirection", "asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(0)))
                .andExpect(jsonPath("$.offset").value(4))
                .andExpect(jsonPath("$.perPage").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }


    @Test
    @Transactional
    public void add_user_to_project() throws Exception {

        Project project = builder.given_a_project();
        User user = builder.given_a_user();
        restUserControllerMockMvc.perform(post("/api/project/{project}/user/{user}.json", project.getId(), user.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isFalse();

    }

    @Test
    @Transactional
    public void add_users_to_project() throws Exception {

        Project project = builder.given_a_project();
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();
        restUserControllerMockMvc.perform(post("/api/project/{project}/user.json", project.getId())
                        .param("users", user1.getId() + "," + user2.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(permissionService.hasACLPermission(project, user1.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project, user1.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, user2.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project, user2.getUsername(), ADMINISTRATION)).isFalse();
    }

    @Test
    @Transactional
    public void add_users_to_project_works_even_if_a_bad_user_id_is_given() throws Exception {

        Project project = builder.given_a_project();
        User user1 = builder.given_a_user();
        restUserControllerMockMvc.perform(post("/api/project/{project}/user.json", project.getId())
                        .param("users", user1.getId() + ",xxxxxx,0") //bad format + bad id
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isPartialContent());

        assertThat(permissionService.hasACLPermission(project, user1.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project, user1.getUsername(), ADMINISTRATION)).isFalse();
    }


    @Test
    @Transactional
    public void delete_user_from_project() throws Exception {

        Project project = builder.given_a_project();
        User user = builder.given_a_user();
        builder.addUserToProject(project, user.getUsername(), READ);
        restUserControllerMockMvc.perform(delete("/api/project/{project}/user/{user}.json", project.getId(), user.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isFalse();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isFalse();

    }

    @Test
    @Transactional
    public void delete_users_from_project() throws Exception {

        Project project = builder.given_a_project();
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();
        builder.addUserToProject(project, user1.getUsername(), READ);
        builder.addUserToProject(project, user2.getUsername(), READ);
        restUserControllerMockMvc.perform(delete("/api/project/{project}/user.json", project.getId())
                        .param("users", user1.getId() + "," + user2.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(permissionService.hasACLPermission(project, user1.getUsername(), READ)).isFalse();
        assertThat(permissionService.hasACLPermission(project, user1.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, user2.getUsername(), READ)).isFalse();
        assertThat(permissionService.hasACLPermission(project, user2.getUsername(), ADMINISTRATION)).isFalse();
    }

    @Test
    @Transactional
    public void delete_users_to_project_works_even_if_a_bad_user_id_is_given() throws Exception {

        Project project = builder.given_a_project();
        User user1 = builder.given_a_user();
        restUserControllerMockMvc.perform(delete("/api/project/{project}/user.json", project.getId())
                        .param("users", user1.getId() + ",xxxxxx,0") //bad format + bad id
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isPartialContent());

        assertThat(permissionService.hasACLPermission(project, user1.getUsername(), READ)).isFalse();
        ;
    }


    @Test
    @Transactional
    public void add_admin_to_project() throws Exception {

        Project project = builder.given_a_project();
        User user = builder.given_a_user();
        restUserControllerMockMvc.perform(post("/api/project/{project}/user/{user}/admin.json", project.getId(), user.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isTrue();

    }

    @Test
    @Transactional
    public void delete_admin_from_project() throws Exception {

        Project project = builder.given_a_project();
        User user = builder.given_a_user();
        builder.addUserToProject(project, user.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, user.getUsername(), READ);
        restUserControllerMockMvc.perform(delete("/api/project/{project}/user/{user}/admin.json", project.getId(), user.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isFalse();

    }

    @Test
    @Transactional
    public void add_user_to_storage() throws Exception {

        Storage storage = builder.given_a_storage();
        User user = builder.given_a_user();
        restUserControllerMockMvc.perform(post("/api/storage/{storage}/user/{user}.json", storage.getId(), user.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();

    }

    @Test
    @Transactional
    public void delete_user_from_storage() throws Exception {

        Storage storage = builder.given_a_storage();
        User user = builder.given_a_user();
        builder.addUserToStorage(storage, user.getUsername(), READ);
        restUserControllerMockMvc.perform(delete("/api/storage/{storage}/user/{user}.json", storage.getId(), user.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isFalse();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();

    }


    @Test
    @Transactional
    public void list_friends() throws Exception {
        User projectPrepresentative = builder.given_a_user();
        User projectAdmin = builder.given_a_user();
        User projectUser = builder.given_a_user();
        User simpleUser = builder.given_a_user();
        Project project = builder.given_a_project();

        builder.addUserToProject(project, projectPrepresentative.getUsername(), ADMINISTRATION);
        builder.given_a_project_representative_user(project, projectPrepresentative);
        builder.addUserToProject(project, projectAdmin.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, projectUser.getUsername(), READ);

        restUserControllerMockMvc.perform(get("/api/user/{id}/friends.json", projectPrepresentative.getId())
                        .param("project", project.getId().toString())
                        .param("offline", "true")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectPrepresentative.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectAdmin.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + simpleUser.getUsername() + "')]").doesNotExist());

        restUserControllerMockMvc.perform(get("/api/user/{id}/friends.json", projectPrepresentative.getId())
                        .param("project", project.getId().toString())
                        .param("offline", "false")
                )
                .andExpect(status().isOk());

        restUserControllerMockMvc.perform(get("/api/user/{id}/friends.json", projectPrepresentative.getId())
                        .param("offline", "false")
                )
                .andExpect(status().isOk());

        restUserControllerMockMvc.perform(get("/api/user/{id}/friends.json", projectPrepresentative.getId())
                        .param("offline", "true")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectPrepresentative.getUsername() + "')]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectAdmin.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + projectUser.getUsername() + "')]").exists())
                .andExpect(jsonPath("$.collection[?(@.username=='" + simpleUser.getUsername() + "')]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_online_users() throws Exception {
        User userOnline = builder.given_default_user();
        User userOnlineButOnDifferentProject = builder.given_a_user();
        User userOffline = builder.given_a_user();

        Project project = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        given_a_last_connection(userOffline, project.getId(), DateUtils.addDays(new Date(), -15));
        given_a_last_connection(userOnline, project.getId(), DateUtils.addSeconds(new Date(), -15));
        given_a_last_connection(userOnlineButOnDifferentProject, anotherProject.getId(), DateUtils.addSeconds(new Date(), -10));

        PersistentUserPosition persistentUserPosition = given_a_persistent_user_position(DateUtils.addSeconds(new Date(), -15), userOnline,
                builder.given_a_not_persisted_slice_instance(builder.given_an_image_instance(project), builder.given_an_abstract_slice()), UserPositionServiceTests.USER_VIEW);


        restUserControllerMockMvc.perform(get("/api/project/{id}/online/user.json", project.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[0].id").value(userOnline.getId()))
                .andExpect(jsonPath("$.collection[0].position").hasJsonPath())
                .andExpect(jsonPath("$.collection[0].position[0].image").value(persistentUserPosition.getImage()))
                .andExpect(jsonPath("$.collection[0].position[0].filename").hasJsonPath())
                .andExpect(jsonPath("$.collection[0].position[0].originalFilename").hasJsonPath())
                .andExpect(jsonPath("$.collection[0].position[0].date").value(persistentUserPosition.getCreated().getTime()));
    }

    @Test
    @Transactional
    public void list_user_activity() throws Exception {

        User userOnline = builder.given_default_user();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, userOnline.getUsername());

        PersistentProjectConnection lastConnection = given_a_persistent_connection_in_project(userOnline, project, DateUtils.addSeconds(new Date(), -15));

        PersistentImageConsultation consultation = given_a_persistent_image_consultation(userOnline, builder.given_an_image_instance(project), new Date());

        restUserControllerMockMvc.perform(get("/api/project/{id}/usersActivity.json", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[0].id").value(userOnline.getId()))
                .andExpect(jsonPath("$.collection[0].username").value(userOnline.getUsername()))
                .andExpect(jsonPath("$.collection[0].name").value(userOnline.getName()))
                .andExpect(jsonPath("$.collection[0].fullName").value(userOnline.getFullName()))
                .andExpect(jsonPath("$.collection[0].lastImageId").value(consultation.getImage()))
                .andExpect(jsonPath("$.collection[0].lastImageName").hasJsonPath())
                .andExpect(jsonPath("$.collection[0].lastConnection").value(lastConnection.getCreated().getTime()))
                .andExpect(jsonPath("$.collection[0].frequency").value(1));
    }

    @Test
    void get_resume_activity() throws Exception {
        User userOnline = builder.given_default_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, userOnline.getUsername());

        PersistentProjectConnection firstConnection = given_a_persistent_connection_in_project(userOnline, project, DateUtils.addDays(new Date(), -15));
        PersistentProjectConnection lastConnection = given_a_persistent_connection_in_project(userOnline, project, DateUtils.addSeconds(new Date(), -15));

        given_a_persistent_image_consultation(userOnline, builder.given_an_image_instance(project), new Date());

        restUserControllerMockMvc.perform(get("/api/project/{id}/resumeActivity/{user}.json", project.getId(), userOnline.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstConnection").value(firstConnection.getCreated().getTime()))
                .andExpect(jsonPath("$.lastConnection").value(lastConnection.getCreated().getTime()))
                .andExpect(jsonPath("$.totalAnnotations").value(0))
                .andExpect(jsonPath("$.totalConnections").value(2))
                .andExpect(jsonPath("$.totalConsultations").value(1))
                .andExpect(jsonPath("$.totalAnnotationSelections").value(0));

    }

    @Test
    @Transactional
    public void download_user_list_from_project_xls_document() throws Exception {
        User user = builder.given_a_user("Paul");
        Project project = builder.given_a_project_with_user(user);
        MvcResult mvcResult = performDownload("xls", project, "application/octet-stream");
        checkXLSResult(mvcResult, user);
    }

    @Test
    @Transactional
    public void download_user_list_from_project_csv_document() throws Exception {
        User user = builder.given_a_user("Paul");
        Project project = builder.given_a_project_with_user(user);
        MvcResult mvcResult = performDownload("csv", project, "text/csv");
        checkResult(";", mvcResult, user);
    }

    @Test
    @Transactional
    public void download_user_list_from_project_pdf_document() throws Exception {
        Project project = builder.given_a_project_with_user(builder.given_a_user());
        performDownload("pdf", project, "application/pdf");
    }

    private void checkResult(String delimiter, MvcResult result, User user) throws UnsupportedEncodingException {
        String[] rows = result.getResponse().getContentAsString().split("\r\n|\r|\n");
        String[] userAnnotationResult = rows[1].split(delimiter);
        AssertionsForClassTypes.assertThat(userAnnotationResult[0]).isEqualTo(user.getUsername());
        AssertionsForClassTypes.assertThat(userAnnotationResult[1]).isEqualTo(user.getName());
    }

    private void checkXLSResult(MvcResult result, User user) throws IOException {
        byte[] spreadsheetData = result.getResponse().getContentAsByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(spreadsheetData);
        Workbook workbook = null;

        workbook = new HSSFWorkbook(inputStream);

        Sheet sheet = workbook.getSheetAt(0);

        Row row = sheet.getRow(1); // Assuming the data starts from the second row
        Cell[] cells = new Cell[row.getLastCellNum()];
        for (int i = 0; i < row.getLastCellNum(); i++) {
            cells[i] = row.getCell(i);
        }

        AssertionsForClassTypes.assertThat(cells[0].getStringCellValue()).isEqualTo(user.getUsername());
        AssertionsForClassTypes.assertThat(cells[1].getStringCellValue()).isEqualTo(user.getName());


        workbook.close();

    }

    private MvcResult performDownload(String format, Project project, String type) throws Exception {
        return restUserControllerMockMvc.perform(get("/api//project/{project}/user/download", project.getId())
                        .param("format", format))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", type))
                .andReturn();
    }
}
