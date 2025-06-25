package be.cytomine.service.ontology;

import jakarta.transaction.Transactional;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class AnnotationGroupServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    AnnotationGroupService annotationGroupService;

    @Test
    void find_annotation_group_with_success() {
        AnnotationGroup annotationGroup = builder.given_an_annotation_group();
        AssertionsForClassTypes.assertThat(annotationGroupService.find(annotationGroup.getId()).isPresent());
        assertThat(annotationGroup).isEqualTo(annotationGroupService.find(annotationGroup.getId()).get());
    }

    @Test
    void find_non_existing_annotation_group_return_empty() {
        AssertionsForClassTypes.assertThat(annotationGroupService.find(0L)).isEmpty();
    }

    @Test
    void get_non_existing_annotation_group_return_null() {
        AssertionsForClassTypes.assertThat(annotationGroupService.get(0L)).isNull();
    }

    @Test
    void list_annotation_group_by_project() {
        Project project = builder.given_a_project();
        ImageGroup imageGroup = builder.given_an_imagegroup(project);

        AnnotationGroup annotationGroup1 = builder.given_an_annotation_group(project, imageGroup);
        AnnotationGroup annotationGroup2 = builder.given_an_annotation_group(project, imageGroup);
        AnnotationGroup annotationGroup3 = builder.given_an_annotation_group(project, imageGroup);
        AnnotationGroup annotationGroup4 = builder.given_an_annotation_group();

        AssertionsForInterfaceTypes.assertThat(annotationGroupService.list(project)).containsExactly(annotationGroup1, annotationGroup2, annotationGroup3);
        AssertionsForInterfaceTypes.assertThat(annotationGroupService.list(project)).doesNotContain(annotationGroup4);
    }

    @Test
    void list_annotation_group_by_image_group() {
        Project project = builder.given_a_project();
        ImageGroup imageGroup = builder.given_an_imagegroup(project);

        AnnotationGroup annotationGroup1 = builder.given_an_annotation_group(project, imageGroup);
        AnnotationGroup annotationGroup2 = builder.given_an_annotation_group(project, imageGroup);
        AnnotationGroup annotationGroup3 = builder.given_an_annotation_group(project, imageGroup);
        AnnotationGroup annotationGroup4 = builder.given_an_annotation_group();

        AssertionsForInterfaceTypes.assertThat(annotationGroupService.list(imageGroup)).containsExactly(annotationGroup1, annotationGroup2, annotationGroup3);
        AssertionsForInterfaceTypes.assertThat(annotationGroupService.list(imageGroup)).doesNotContain(annotationGroup4);
    }

    @Test
    void add_valid_annotation_group_with_success() {
        AnnotationGroup annotationGroup = builder.given_a_not_persisted_annotation_group();

        CommandResponse commandResponse = annotationGroupService.add(annotationGroup.toJsonObject());

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(annotationGroupService.find(commandResponse.getObject().getId())).isPresent();
    }

    @Test
    void add_annotation_group_with_null_project_fails() {
        AnnotationGroup annotationGroup = builder.given_an_annotation_group();
        Assertions.assertThrows(ObjectNotFoundException.class, () -> annotationGroupService.add(annotationGroup.toJsonObject().withChange("project", null)));
    }

    @Test
    void edit_annotation_group_with_success() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        ImageGroup imageGroup = builder.given_an_imagegroup(project1);
        AnnotationGroup annotationGroup = builder.given_an_annotation_group(project1, imageGroup);

        JsonObject jsonObject = annotationGroup.toJsonObject();
        jsonObject.put("project", project2.getId());

        CommandResponse commandResponse = annotationGroupService.edit(jsonObject, true);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(annotationGroupService.find(commandResponse.getObject().getId())).isPresent();

        AnnotationGroup updated = annotationGroupService.find(commandResponse.getObject().getId()).get();
        assertThat(updated.getProject()).isEqualTo(project2);
    }

    @Test
    void delete_annotation_group_with_success() {
        AnnotationGroup annotationGroup = builder.given_an_annotation_group();

        CommandResponse commandResponse = annotationGroupService.delete(annotationGroup, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(annotationGroupService.find(annotationGroup.getId()).isEmpty());
    }

    @Test
    public void merge_annotation_group_with_success() {
        Project project = builder.given_a_project();
        ImageGroup imageGroup = builder.given_an_imagegroup(project);
        AnnotationGroup annotationGroup = builder.given_an_annotation_group(project, imageGroup);
        AnnotationGroup annotationGroupToMerge = builder.given_an_annotation_group(project, imageGroup);

        CommandResponse commandResponse = annotationGroupService.merge(annotationGroup.getId(), annotationGroupToMerge.getId());
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(annotationGroupService.find(annotationGroupToMerge.getId()).isEmpty());
        AssertionsForClassTypes.assertThat(annotationGroupService.find(annotationGroup.getId()).isPresent());
    }
}

