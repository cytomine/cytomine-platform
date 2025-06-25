package be.cytomine.service.image.group;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.image.group.ImageGroupImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.utils.CommandResponse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class ImageGroupImageInstanceServiceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private ImageGroupImageInstanceService imageGroupImageInstanceService;

    @Test
    void get_non_existing_imagegroup_imageinstance_return_null() {
        Project project = builder.given_a_project();
        ImageGroup group = builder.given_an_imagegroup(project);
        ImageInstance image = builder.given_an_image_instance(project);
        AssertionsForClassTypes.assertThat(imageGroupImageInstanceService.get(group, image)).isNull();
    }

    @Test
    void find_imagegroup_imageinstance_with_success() {
        ImageGroupImageInstance igii = builder.given_an_imagegroup_imageinstance();
        AssertionsForClassTypes.assertThat(imageGroupImageInstanceService.find(igii.getGroup(), igii.getImage()).isPresent());
        assertThat(igii).isEqualTo(imageGroupImageInstanceService.find(igii.getGroup(), igii.getImage()).get());
    }

    @Test
    void list_imagegroup_imageinstance_by_imageinstance() {
        Project project = builder.given_a_project();
        ImageGroup group = builder.given_an_imagegroup(project);
        ImageInstance image = builder.given_an_image_instance(project);

        ImageGroupImageInstance igii1 = builder.given_an_imagegroup_imageinstance(group, image);
        ImageGroupImageInstance igii2 = builder.given_an_imagegroup_imageinstance(group, image);
        ImageGroupImageInstance igii3 = builder.given_an_imagegroup_imageinstance(group, image);
        ImageGroupImageInstance igii4 = builder.given_an_imagegroup_imageinstance();

        AssertionsForInterfaceTypes.assertThat(imageGroupImageInstanceService.list(image)).containsExactly(igii1, igii2, igii3);
        AssertionsForInterfaceTypes.assertThat(imageGroupImageInstanceService.list(image)).doesNotContain(igii4);
    }

    @Test
    void list_imagegroup_imageinstance_by_imagegroup() {
        Project project = builder.given_a_project();
        ImageGroup group = builder.given_an_imagegroup(project);
        ImageInstance image = builder.given_an_image_instance(project);

        ImageGroupImageInstance igii1 = builder.given_an_imagegroup_imageinstance(group, image);
        ImageGroupImageInstance igii2 = builder.given_an_imagegroup_imageinstance(group, image);
        ImageGroupImageInstance igii3 = builder.given_an_imagegroup_imageinstance(group, image);
        ImageGroupImageInstance igii4 = builder.given_an_imagegroup_imageinstance();

        AssertionsForInterfaceTypes.assertThat(imageGroupImageInstanceService.list(group)).containsExactly(igii1, igii2, igii3);
        AssertionsForInterfaceTypes.assertThat(imageGroupImageInstanceService.list(group)).doesNotContain(igii4);
    }

    @Test
    void add_valid_imagegroup_imageinstance_with_success() {
        ImageGroupImageInstance igii = builder.given_a_not_persisted_imagegroup_imageinstance();

        CommandResponse commandResponse = imageGroupImageInstanceService.add(igii.toJsonObject());

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        ImageGroupImageInstance response = (ImageGroupImageInstance) commandResponse.getObject();
        AssertionsForClassTypes.assertThat(imageGroupImageInstanceService.find(response.getGroup(), response.getImage())).isPresent();
    }

    @Test
    void add_imagegroup_imageinstance_with_wrong_group_fails() {
        ImageGroupImageInstance igii = builder.given_an_imagegroup_imageinstance();
        Assertions.assertThrows(WrongArgumentException.class, () -> imageGroupImageInstanceService.add(igii.toJsonObject().withChange("group", builder.given_an_imagegroup().getId())));
    }

    @Test
    void add_imagegroup_imageinstance_with_wrong_image_fails() {
        ImageGroupImageInstance igii = builder.given_an_imagegroup_imageinstance();
        Assertions.assertThrows(WrongArgumentException.class, () -> imageGroupImageInstanceService.add(igii.toJsonObject().withChange("image", builder.given_an_image_instance().getId())));
    }

    @Test
    void delete_imagegroup_imageinstance_with_success() {
        ImageGroupImageInstance igii = builder.given_an_imagegroup_imageinstance();

        CommandResponse commandResponse = imageGroupImageInstanceService.delete(igii, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(imageGroupImageInstanceService.find(igii.getGroup(), igii.getImage()).isEmpty());
    }
}
