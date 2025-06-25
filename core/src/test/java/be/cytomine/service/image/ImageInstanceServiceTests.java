package be.cytomine.service.image;

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

import java.util.*;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.test.context.support.WithMockUser;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.NestedImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Description;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.image.ImageInstanceBounds;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repositorynosql.social.AnnotationActionRepository;
import be.cytomine.repositorynosql.social.PersistentImageConsultationRepository;
import be.cytomine.repositorynosql.social.PersistentUserPositionRepository;
import be.cytomine.service.search.ImageSearchExtension;
import be.cytomine.service.social.AnnotationActionService;
import be.cytomine.service.social.ImageConsultationService;
import be.cytomine.service.social.UserPositionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;

import static be.cytomine.service.social.UserPositionServiceTests.USER_VIEW;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class ImageInstanceServiceTests {

    @Autowired
    ImageInstanceService imageInstanceService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    EntityManager entityManager;

    @Autowired
    AnnotationActionRepository annotationActionRepository;

    @Autowired
    AnnotationActionService annotationActionService;

    @Autowired
    UserPositionService userPositionService;

    @Autowired
    PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    ImageConsultationService imageConsultationService;

    @Autowired
    PersistentImageConsultationRepository persistentImageConsultationRepository;

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void beforeAll() {
        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    public static void afterAll() {
        wireMockServer.stop();
    }

    @BeforeEach
    public void cleanDB() {
        persistentImageConsultationRepository.deleteAll();
        annotationActionRepository.deleteAll();
        persistentUserPositionRepository.deleteAll();
    }

    @Test
    void retrieve_image_bounds_for_empty_project() {
        Project project = builder.given_a_project();
        ImageInstanceBounds imageInstanceBounds = imageInstanceService.computeBounds(project);
        assertThat(imageInstanceBounds).isNotNull();
    }

    @Test
    void retrieve_image_bounds() {
        Project project = builder.given_a_project();

        List<Date> dateChoices = new ArrayList<>(List.of(
                new GregorianCalendar(2021, Calendar.JANUARY, 1).getTime(),
                new GregorianCalendar(2021, Calendar.JULY, 1).getTime(),
                new GregorianCalendar(2021, Calendar.DECEMBER, 1).getTime()
        ));
        Collections.shuffle(dateChoices);

        List<Integer> intChoices = new ArrayList<>(List.of(1,2,3));
        Collections.shuffle(intChoices);
        List<Double> doubleChoices = new ArrayList<>(List.of(0.5, 10.1, 99.99));
        Collections.shuffle(doubleChoices);
        List<String> stringChoices = new ArrayList<>(List.of("aaa", "zzzz", "AAAA"));
        Collections.shuffle(stringChoices);


        for (int k = 0 ; k < 2 ; k++) { // execute twice the creation of images (6 images)
            for (int i = 0; i < 3; i++) {
                ImageInstance imageInstance = builder.given_an_image_instance(project);
                imageInstance.setUpdated(dateChoices.get(i));
                imageInstance.setReviewStart(dateChoices.get(i));
                imageInstance.setReviewStop(dateChoices.get(i));

                imageInstance.setMagnification(intChoices.get(i));
                imageInstance.setPhysicalSizeX(doubleChoices.get(i));
                imageInstance.setPhysicalSizeY(doubleChoices.get(i));
                imageInstance.setPhysicalSizeZ(doubleChoices.get(i));
                imageInstance.setFps(doubleChoices.get(i));

                imageInstance.getBaseImage().getUploadedFile().setContentType(stringChoices.get(i));

                imageInstance.setCountImageAnnotations((long)intChoices.get(i));
                imageInstance.setCountImageReviewedAnnotations((long)intChoices.get(i));
                imageInstance.setCountImageJobAnnotations((long)intChoices.get(i));

                imageInstance.getBaseImage().setWidth(intChoices.get(i));
                imageInstance.getBaseImage().setHeight(intChoices.get(i));

                builder.persistAndReturn(imageInstance);
            }
        }

        ImageInstance imageInstanceWithNullValues = builder.given_an_image_instance(project);
        imageInstanceWithNullValues.setReviewStart(null);
        imageInstanceWithNullValues.setReviewStop(null);
        imageInstanceWithNullValues.setMagnification(null);
        imageInstanceWithNullValues.setPhysicalSizeX(null);
        imageInstanceWithNullValues.setPhysicalSizeY(null);
        imageInstanceWithNullValues.setPhysicalSizeZ(null);
        imageInstanceWithNullValues.setFps(null);
        imageInstanceWithNullValues.getBaseImage().setWidth(null);
        imageInstanceWithNullValues.getBaseImage().setHeight(null);
        builder.persistAndReturn(imageInstanceWithNullValues);

        ImageInstanceBounds imageInstanceBounds = imageInstanceService.computeBounds(project);

        //Created cannot be set (auto generated)
//        assertThat(imageInstanceBounds.getCreated().getMin()).isEqualTo(new GregorianCalendar(2021, Calendar.JANUARY, 1).getTime());
//        assertThat(imageInstanceBounds.getCreated().getMax()).isEqualTo(new GregorianCalendar(2021, Calendar.DECEMBER, 1).getTime());
        assertThat(imageInstanceBounds.getReviewStart().getMin()).isEqualTo(new GregorianCalendar(2021, Calendar.JANUARY, 1).getTime());
        assertThat(imageInstanceBounds.getReviewStart().getMax()).isEqualTo(new GregorianCalendar(2021, Calendar.DECEMBER, 1).getTime());
        assertThat(imageInstanceBounds.getReviewStop().getMin()).isEqualTo(new GregorianCalendar(2021, Calendar.JANUARY, 1).getTime());
        assertThat(imageInstanceBounds.getReviewStop().getMax()).isEqualTo(new GregorianCalendar(2021, Calendar.DECEMBER, 1).getTime());

        assertThat(imageInstanceBounds.getMagnification().getMin()).isEqualTo(1);
        assertThat(imageInstanceBounds.getMagnification().getMax()).isEqualTo(3);
        assertThat(imageInstanceBounds.getWidth().getMin()).isEqualTo(1);
        assertThat(imageInstanceBounds.getWidth().getMax()).isEqualTo(3);
        assertThat(imageInstanceBounds.getHeight().getMin()).isEqualTo(1);
        assertThat(imageInstanceBounds.getHeight().getMax()).isEqualTo(3);

        assertThat(imageInstanceBounds.getFps().getMin()).isEqualTo(0.5);
        assertThat(imageInstanceBounds.getFps().getMax()).isEqualTo(99.99);
        assertThat(imageInstanceBounds.getPhysicalSizeX().getMin()).isEqualTo(0.5);
        assertThat(imageInstanceBounds.getPhysicalSizeX().getMax()).isEqualTo(99.99);
        assertThat(imageInstanceBounds.getPhysicalSizeY().getMin()).isEqualTo(0.5);
        assertThat(imageInstanceBounds.getPhysicalSizeY().getMax()).isEqualTo(99.99);
        assertThat(imageInstanceBounds.getPhysicalSizeY().getMin()).isEqualTo(0.5);
        assertThat(imageInstanceBounds.getPhysicalSizeY().getMax()).isEqualTo(99.99);

        assertThat(imageInstanceBounds.getCountImageAnnotations().getMin()).isEqualTo(0L); //special case since default value is 0
        assertThat(imageInstanceBounds.getCountImageAnnotations().getMax()).isEqualTo(3L);
        assertThat(imageInstanceBounds.getCountImageJobAnnotations().getMin()).isEqualTo(0L); //special case since default value is 0
        assertThat(imageInstanceBounds.getCountImageJobAnnotations().getMax()).isEqualTo(3L);
        assertThat(imageInstanceBounds.getCountImageReviewedAnnotations().getMin()).isEqualTo(0L); //special case since default value is 0
        assertThat(imageInstanceBounds.getCountImageReviewedAnnotations().getMax()).isEqualTo(3L);


        assertThat(imageInstanceBounds.getMagnification().getList()).contains(1,2,3);
        assertThat(imageInstanceBounds.getMimeType().getList()).contains("aaa", "zzzz", "AAAA");
        assertThat(imageInstanceBounds.getFormat().getList()).contains("aaa", "zzzz", "AAAA");
    }


    @Test
    void list_all_image_by_projects() {
        ImageInstance imageInstance1 = builder.given_an_image_instance();
        builder.persistAndReturn(imageInstance1);
        ImageInstance imageInstance2 = builder.given_an_image_instance();
        builder.persistAndReturn(imageInstance2);

        Page<Map<String, Object>> page = imageInstanceService.list(imageInstance1.getProject(), new ArrayList<>(), "id", "asc", 0L, 0L, false, false);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(imageInstance1.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(imageInstance2.getId());
    }

    @Test
    void list_all_image_by_projects_ignore_nested_image_instance() {
        ImageInstance imageInstance1 = builder.given_an_image_instance();
        NestedImageInstance nestedImageInstance1 = builder.given_a_nested_image_instance();
        nestedImageInstance1.setProject(imageInstance1.getProject());
        builder.persistAndReturn(nestedImageInstance1);
        Page<Map<String, Object>> page = imageInstanceService.list(imageInstance1.getProject(), new ArrayList<>(), "id", "asc", 0L, 0L, false, false);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(imageInstance1.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(nestedImageInstance1.getId());
    }

    @Test
    void search_images_with_last_activities() {
        Date consultation = new Date();
        ImageInstance imageInstance1 = builder.given_an_image_instance();
        ImageInstance imageInstance2 = builder.given_an_image_instance(imageInstance1.getProject());
        imageConsultationService.add(builder.given_superadmin(), imageInstance1.getId(), "xxx", "view", consultation);

        ImageSearchExtension imageSearchExtension = new ImageSearchExtension();
        imageSearchExtension.setWithLastActivity(true);
        Page<Map<String, Object>> results = imageInstanceService.listExtended(imageInstance1.getProject(), imageSearchExtension, new ArrayList<>(), "created", "desc", 0L, 0L);

        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent().get(0).get("id")).isEqualTo(imageInstance2.getId());
        assertThat(results.getContent().get(0).get("lastActivity")).isNull();
        assertThat(results.getContent().get(1).get("id")).isEqualTo(imageInstance1.getId());
        assertThat(results.getContent().get(1).get("lastActivity")).isEqualTo(consultation);
    }

    @Test
    void list_all_image_by_project_light() {
        ImageInstance imageInstance1 = builder.given_an_image_instance();
        builder.persistAndReturn(imageInstance1);
        ImageInstance imageInstance2 = builder.given_an_image_instance();
        builder.persistAndReturn(imageInstance2);

        Page<Map<String, Object>> page = imageInstanceService.list(imageInstance1.getProject(), new ArrayList<>(), "id", "asc", 0L, 0L, true, false);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(imageInstance1.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(imageInstance2.getId());
    }

    @Test
    void list_all_image_by_project_light_ignore_nested_image_instance() {
        ImageInstance imageInstance1 = builder.given_an_image_instance();
        builder.persistAndReturn(imageInstance1);
        NestedImageInstance nestedImageInstance1 = builder.given_a_nested_image_instance();
        nestedImageInstance1.setProject(imageInstance1.getProject());
        builder.persistAndReturn(nestedImageInstance1);

        Page<Map<String, Object>> page = imageInstanceService.list(imageInstance1.getProject(), new ArrayList<>(), "id", "asc", 0L, 0L, true, false);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(imageInstance1.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(nestedImageInstance1.getId());
    }

    @Test
    @WithMockUser("list_by_user_with_search")
    void list_by_user_with_search() {
        User user = builder.given_a_user("list_by_user_with_search");
        Project project = builder.given_a_project();
        builder.addUserToProject(project, user.getUsername(), BasePermission.ADMINISTRATION);
        ImageInstance img1 = builder.given_an_image_instance(project);
        img1.getBaseImage().setWidth(499);
        img1.setInstanceFilename("TEST");
        img1.setCountImageAnnotations(1000L);

        ImageInstance img2 = builder.given_an_image_instance(project);
        img2.getBaseImage().setWidth(501);


        assertThat(imageInstanceService.list(user, new ArrayList<>()).stream().map(x -> x.get("id")))
                .contains(img1.getId(), img2.getId());


        List<SearchParameterEntry> searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("width", SearchOperation.lte, 500),
                        new SearchParameterEntry("numberOfAnnotations", SearchOperation.lte, 1000))
                );
        assertThat(imageInstanceService.list(user, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("numberOfAnnotations", SearchOperation.gte, 1))
                );
        assertThat(imageInstanceService.list(user, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("numberOfAnnotations", SearchOperation.gte, 1000L),
                        new SearchParameterEntry("numberOfAnnotations", SearchOperation.lte, 1000L))
                );
        assertThat(imageInstanceService.list(user, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());


        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("width", SearchOperation.lte, 1000)
                ));
        assertThat(imageInstanceService.list(user, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId(), img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("width", SearchOperation.gte, 1000)
                ));
        assertThat(imageInstanceService.list(user, searchParameterEntryList).stream().map(x -> x.get("id")))
                .doesNotContain(img1.getId(), img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("baseImage", SearchOperation.equals, img1.getBaseImage().getId())
                ));
        assertThat(imageInstanceService.list(user, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("name", SearchOperation.ilike, img1.getInstanceFilename())
                ));
        assertThat(imageInstanceService.list(user, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());
    }


    @Test
    void list_by_project_with_search() {
        Project project = builder.given_a_project();
        ImageInstance img1 = builder.given_an_image_instance(project);
        img1.getBaseImage().setWidth(499);
        img1.setInstanceFilename("TEST");
        img1.setCountImageAnnotations(1000L);
        TagDomainAssociation tagForImage1 = builder.given_a_tag_association(builder.given_a_tag("xxx"), img1);

        ImageInstance img2 = builder.given_an_image_instance(project);
        img2.getBaseImage().setWidth(501);

        assertThat(imageInstanceService.list(project, new ArrayList<>()).stream().map(x -> x.get("id")))
                .contains(img1.getId(), img2.getId());


        List<SearchParameterEntry> searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("width", SearchOperation.lte, 500),
                        new SearchParameterEntry("numberOfAnnotations", SearchOperation.lte, 1000))
                );
        assertThat(imageInstanceService.list(project, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("numberOfAnnotations", SearchOperation.gte, 1))
                );
        assertThat(imageInstanceService.list(project, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());


        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("width", SearchOperation.lte, 1000)
                ));
        assertThat(imageInstanceService.list(project, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId(), img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("width", SearchOperation.gte, 1000)
                ));
        assertThat(imageInstanceService.list(project, searchParameterEntryList).stream().map(x -> x.get("id")))
                .doesNotContain(img1.getId(), img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("baseImage", SearchOperation.equals, img1.getBaseImage().getId())
                ));
        assertThat(imageInstanceService.list(project, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("name", SearchOperation.ilike, img1.getInstanceFilename())
                ));
        assertThat(imageInstanceService.list(project, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("tag", SearchOperation.in, List.of(tagForImage1.getTag().getId()))
                ));
        assertThat(imageInstanceService.list(project, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());
    }

    @Test
    @WithMockUser("list_by_project_with_search_with_blind_mode")
    void list_by_project_with_search_with_blind_mode() {
        User user = builder.given_a_user("list_by_project_with_search_with_blind_mode");
        Project project = builder.given_a_project();
        builder.addUserToProject(project, user.getUsername(), BasePermission.WRITE);
        project.setBlindMode(true);
        ImageInstance img1 = builder.given_an_image_instance(project);
        img1.setInstanceFilename("TEST");

        ImageInstance img2 = builder.given_an_image_instance(project);
        img2.getBaseImage().setWidth(501);

        assertThat(imageInstanceService.list(project, new ArrayList<>()).stream().map(x -> x.get("id")))
                .contains(img1.getId(), img2.getId());


        List<SearchParameterEntry> searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("instanceFilename", SearchOperation.ilike, img1.getInstanceFilename())
                ));
        assertThat(imageInstanceService.list(project, searchParameterEntryList).stream().map(x -> x.get("id")))
                .doesNotContain(img1.getId(), img2.getId());

        project.setBlindMode(false);

        searchParameterEntryList =
                new ArrayList<>(List.of(
                        new SearchParameterEntry("instanceFilename", SearchOperation.ilike, img1.getInstanceFilename())
                ));
        assertThat(imageInstanceService.list(project, searchParameterEntryList).stream().map(x -> x.get("id")))
                .contains(img1.getId()).doesNotContain(img2.getId());

    }


    @Test
    void list_all_image_ids_for_project() {
        ImageInstance imageInstance1 = builder.given_an_image_instance();
        builder.persistAndReturn(imageInstance1);
        ImageInstance imageInstance2 = builder.given_an_image_instance();
        builder.persistAndReturn(imageInstance2);

        assertThat(imageInstanceService.getAllImageId(imageInstance1.getProject())).contains(imageInstance1.getId());
        assertThat(imageInstanceService.getAllImageId(imageInstance1.getProject())).doesNotContain(imageInstance2.getId());
    }

    @Test
    void list_all_image_ids_for_project_ignore_nested_image() {
        ImageInstance imageInstance1 = builder.given_an_image_instance();
        builder.persistAndReturn(imageInstance1);
        NestedImageInstance nestedImageInstance1 = builder.given_a_nested_image_instance();
        nestedImageInstance1.setProject(imageInstance1.getProject());
        builder.persistAndReturn(nestedImageInstance1);

        assertThat(imageInstanceService.getAllImageId(imageInstance1.getProject())).contains(imageInstance1.getId());
        assertThat(imageInstanceService.getAllImageId(imageInstance1.getProject())).doesNotContain(nestedImageInstance1.getId());
    }

    @Test
    void list_images_with_tree_structure() {
        Project project = builder.given_a_project();
        assertThat(imageInstanceService.listTree(project, 0L, 0L)).isNotNull();
    }


    @Test
    void get_image_intance_with_success() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        assertThat(imageInstance).isEqualTo(imageInstanceService.get(imageInstance.getId()));
    }

    @Test
    void get_unexisting_imageInstance_return_null() {
        AssertionsForClassTypes.assertThat(imageInstanceService.get(0L)).isNull();
    }

    @Test
    void find_imageInstance_with_success() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        AssertionsForClassTypes.assertThat(imageInstanceService.find(imageInstance.getId()).isPresent());
        assertThat(imageInstance).isEqualTo(imageInstanceService.find(imageInstance.getId()).get());
    }

    @Test
    void find_unexisting_imageInstance_return_empty() {
        AssertionsForClassTypes.assertThat(imageInstanceService.find(0L)).isEmpty();
    }


    @Test
    void find_next_image_intance_with_success() {
        Project project = builder.given_a_project();
        ImageInstance imageInstance1 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );
        ImageInstance imageInstance2 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );
        ImageInstance imageInstance3 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );

        assertThat(imageInstanceService.next(imageInstance1)).isEmpty();
        assertThat(imageInstanceService.next(imageInstance2)).isPresent().hasValue(imageInstance1);
        assertThat(imageInstanceService.next(imageInstance3)).isPresent().hasValue(imageInstance2);
    }

    @Test
    void find_previous_image_intance_with_success() {
        Project project = builder.given_a_project();
        ImageInstance imageInstance1 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );
        ImageInstance imageInstance2 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );
        ImageInstance imageInstance3 = builder.given_an_image_instance(
                builder.given_an_abstract_image(), project
        );

        assertThat(imageInstanceService.previous(imageInstance3)).isEmpty();
        assertThat(imageInstanceService.previous(imageInstance2)).isPresent().hasValue(imageInstance3);
        assertThat(imageInstanceService.previous(imageInstance1)).isPresent().hasValue(imageInstance2);
    }

    @Test
    void add_valid_image_instance_with_success() {
        ImageInstance imageInstance = builder.given_a_not_persisted_image_instance();

        CommandResponse commandResponse = imageInstanceService.add(imageInstance.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(imageInstanceService.find(commandResponse.getObject().getId())).isPresent();
        ImageInstance created = imageInstanceService.find(commandResponse.getObject().getId()).get();
    }


    @Test
    void add_already_existing_image_instance_fails() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        Assertions.assertThrows(AlreadyExistException.class, () -> {
            imageInstanceService.add(imageInstance.toJsonObject().withChange("id", null));
        });
    }

    @Test
    void add_valid_image_instance_with_unexsting_abstract_image_fails() {
        ImageInstance imageInstance = builder.given_a_not_persisted_image_instance(null, builder.given_a_project());
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            imageInstanceService.add(imageInstance.toJsonObject());
        });
    }

    @Test
    void edit_image_instance_with_success() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();

        ImageInstance imageInstance = builder.given_a_not_persisted_image_instance(
                builder.given_an_abstract_image(), project1);
        imageInstance = builder.persistAndReturn(imageInstance);

        JsonObject jsonObject = imageInstance.toJsonObject();
        jsonObject.put("project", project2.getId());

        CommandResponse commandResponse = imageInstanceService.edit(jsonObject, true);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(imageInstanceService.find(commandResponse.getObject().getId())).isPresent();
        ImageInstance updated = imageInstanceService.find(commandResponse.getObject().getId()).get();

        assertThat(updated.getProject().getId()).isEqualTo(project2.getId());
    }

    @Test
    void edit_image_instance_magnification_no_impact_in_abstract_image() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstance.getBaseImage().setMagnification(10);
        builder.persistAndReturn(imageInstance.getBaseImage());

        JsonObject jsonObject = imageInstance.toJsonObject();
        jsonObject.put("magnification", 20);

        CommandResponse commandResponse = imageInstanceService.update(imageInstance, jsonObject);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(imageInstanceService.find(commandResponse.getObject().getId())).isPresent();
        ImageInstance updated = imageInstanceService.find(commandResponse.getObject().getId()).get();
        entityManager.refresh(imageInstance);

        assertThat(updated.getMagnification()).isEqualTo(20);
        assertThat(updated.getBaseImage().getMagnification()).isNotEqualTo(20);

    }

    @Test
    void edit_image_instance_resolution_modifies_user_annotation() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.setImage(imageInstance);

        Double perimeter = userAnnotation.getPerimeter();
        Double area = userAnnotation.getArea();

        imageInstanceService.update(imageInstance, imageInstance.toJsonObject().withChange("physicalSizeX", 2.5d));

        assertThat(userAnnotation.getPerimeter()).isNotEqualTo(perimeter);
        assertThat(userAnnotation.getArea()).isNotEqualTo(area);
    }

    @Test
    void edit_image_instance_resolution_modifies_reviewed_annotation() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.setImage(imageInstance);

        Double perimeter = reviewedAnnotation.getPerimeter();
        Double area = reviewedAnnotation.getArea();

        imageInstanceService.update(imageInstance, imageInstance.toJsonObject().withChange("physicalSizeX", 2.5d));

        assertThat(reviewedAnnotation.getPerimeter()).isNotEqualTo(perimeter);
        assertThat(reviewedAnnotation.getArea()).isNotEqualTo(area);
    }

    @Test
    void edit_image_instance_with_unexsting_abstract_image_fails() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            imageInstanceService.add(imageInstance.toJsonObject().withChange("baseImage", null));
        });
    }

    @Test
    void edit_image_instance_with_unexsting_project_fails() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            imageInstanceService.add(imageInstance.toJsonObject().withChange("project", null));
        });
    }


    @Test
    void delete_image_instance_with_success() {
        ImageInstance imageInstance = builder.given_an_image_instance();

        CommandResponse commandResponse = imageInstanceService.delete(imageInstance, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(imageInstanceService.find(imageInstance.getId()).isEmpty());
    }

    @Test
    void delete_image_instance_with_dependencies_with_success() {
        SliceInstance sliceInstance = builder.given_a_slice_instance();
        ImageInstance imageInstance = sliceInstance.getImage();

        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.setImage(imageInstance);

        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.setImage(imageInstance);

        Property property = builder.given_a_property(imageInstance, "mustbedeleted", "value");
        Description description = builder.given_a_description(imageInstance);
        TagDomainAssociation tagDomainAssociation = builder.given_a_tag_association(builder.given_a_tag(), imageInstance);
        AttachedFile attachedFile = builder.given_a_attached_file(imageInstance);

        annotationActionService.add(userAnnotation, builder.given_superadmin(), "view", new Date());
        userPositionService.add(new Date(), builder.given_superadmin(), sliceInstance, imageInstance, USER_VIEW, 0, 0d, false);
        imageConsultationService.add(builder.given_superadmin(), imageInstance.getId(), "xxx", "view", new Date());

        AssertionsForClassTypes.assertThat(entityManager.find(ReviewedAnnotation.class, reviewedAnnotation.getId())).isNotNull();
        AssertionsForClassTypes.assertThat(entityManager.find(UserAnnotation.class, userAnnotation.getId())).isNotNull();
        AssertionsForClassTypes.assertThat(entityManager.find(SliceInstance.class, sliceInstance.getId())).isNotNull();
        AssertionsForClassTypes.assertThat(entityManager.find(Property.class, property.getId())).isNotNull();
        AssertionsForClassTypes.assertThat(entityManager.find(Description.class, description.getId())).isNotNull();
        AssertionsForClassTypes.assertThat(entityManager.find(TagDomainAssociation.class, tagDomainAssociation.getId())).isNotNull();
        AssertionsForClassTypes.assertThat(entityManager.find(AttachedFile.class, attachedFile.getId())).isNotNull();

        assertThat(annotationActionRepository.count()).isEqualTo(1);
        assertThat(persistentImageConsultationRepository.count()).isEqualTo(1);
        assertThat(persistentUserPositionRepository.count()).isEqualTo(1);

        CommandResponse commandResponse = imageInstanceService.delete(imageInstance, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(imageInstanceService.find(imageInstance.getId()).isEmpty());

        AssertionsForClassTypes.assertThat(entityManager.find(ReviewedAnnotation.class, reviewedAnnotation.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(UserAnnotation.class, userAnnotation.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(SliceInstance.class, sliceInstance.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(Property.class, property.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(Description.class, description.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(TagDomainAssociation.class, tagDomainAssociation.getId())).isNull();
        AssertionsForClassTypes.assertThat(entityManager.find(AttachedFile.class, attachedFile.getId())).isNull();

        assertThat(annotationActionRepository.count()).isEqualTo(0);
        assertThat(persistentImageConsultationRepository.count()).isEqualTo(0);
        assertThat(persistentUserPositionRepository.count()).isEqualTo(0);
    }



    @Test
    void project_counter() {
        Project project = builder.given_a_project();

        entityManager.refresh(project);
        assertThat(project.getCountImages()).isEqualTo(0);

        ImageInstance imageInstance = builder.given_an_image_instance(project);

        entityManager.refresh(project);
        assertThat(project.getCountImages()).isEqualTo(1);

        entityManager.remove(imageInstance);
        entityManager.flush();

        entityManager.refresh(project);
        assertThat(project.getCountImages()).isEqualTo(0);

    }






    @Test
    void start_image_reviewing() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        assertThat(imageInstance.getReviewStart()).isNull();
        assertThat(imageInstance.getReviewStop()).isNull();
        assertThat(imageInstance.getReviewUser()).isNull();

        imageInstanceService.startReview(imageInstance);
        assertThat(imageInstance.getReviewStart()).isNotNull();
        assertThat(imageInstance.getReviewStop()).isNull();
        assertThat(imageInstance.getReviewUser()).isNotNull();
    }

    @Test
    void stop_image_reviewing() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstanceService.startReview(imageInstance);
        imageInstanceService.stopReview(imageInstance, false);
        assertThat(imageInstance.getReviewStart()).isNotNull();
        assertThat(imageInstance.getReviewStop()).isNotNull();
        assertThat(imageInstance.getReviewUser()).isNotNull();
    }

    @Test
    void stop_image_reviewing_with_cancel() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstanceService.startReview(imageInstance);
        imageInstanceService.stopReview(imageInstance, true);
        assertThat(imageInstance.getReviewStart()).isNull();
        assertThat(imageInstance.getReviewStop()).isNull();
        assertThat(imageInstance.getReviewUser()).isNull();
    }


}
