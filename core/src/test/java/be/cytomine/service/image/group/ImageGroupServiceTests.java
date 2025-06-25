package be.cytomine.service.image.group;

import jakarta.transaction.Transactional;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class ImageGroupServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    ImageGroupService imageGroupService;

    @Test
    void get_non_existing_imagegroup_return_null() {
        AssertionsForClassTypes.assertThat(imageGroupService.get(0L)).isNull();
    }

    @Test
    void find_imagegroup_with_success() {
        ImageGroup imageGroup = builder.given_an_imagegroup();
        AssertionsForClassTypes.assertThat(imageGroupService.find(imageGroup.getId()).isPresent());
        assertThat(imageGroup).isEqualTo(imageGroupService.find(imageGroup.getId()).get());
    }

    @Test
    void find_non_existing_imagegroup_return_empty() {
        AssertionsForClassTypes.assertThat(imageGroupService.find(0L)).isEmpty();
    }

    @Test
    void list_imagegroup_by_project() {
        Project project = builder.given_a_project();

        ImageGroup imageGroup1 = builder.given_an_imagegroup(project);
        ImageGroup imageGroup2 = builder.given_an_imagegroup(project);
        ImageGroup imageGroup3 = builder.given_an_imagegroup(project);
        ImageGroup imageGroup4 = builder.given_an_imagegroup();

        assertThat(imageGroupService.list(project)).containsExactly(imageGroup1, imageGroup2, imageGroup3);
        assertThat(imageGroupService.list(project)).doesNotContain(imageGroup4);
    }

    @Test
    void add_valid_imagegroup_with_success() {
        ImageGroup imageGroup = builder.given_a_not_persisted_imagegroup();

        CommandResponse commandResponse = imageGroupService.add(imageGroup.toJsonObject());

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(imageGroupService.find(commandResponse.getObject().getId())).isPresent();
    }

    @Test
    void add_imagegroup_with_null_project_fails() {
        ImageGroup imageGroup = builder.given_an_imagegroup();
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            imageGroupService.add(imageGroup.toJsonObject().withChange("project", null));
        });
    }

    @Test
    void edit_imagegroup_with_success() {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        ImageGroup imageGroup = builder.given_an_imagegroup(project1);

        JsonObject jsonObject = imageGroup.toJsonObject();
        jsonObject.put("project", project2.getId());

        CommandResponse commandResponse = imageGroupService.edit(jsonObject, true);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(imageGroupService.find(commandResponse.getObject().getId())).isPresent();
        ImageGroup updated = imageGroupService.find(commandResponse.getObject().getId()).get();

        assertThat(updated.getProject()).isEqualTo(project2);
    }

    @Test
    void delete_imagegroup_with_success() {
        ImageGroup imageGroup = builder.given_an_imagegroup();

        CommandResponse commandResponse = imageGroupService.delete(imageGroup, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(imageGroupService.find(imageGroup.getId()).isEmpty());
    }
}
