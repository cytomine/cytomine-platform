package be.cytomine.service.processing;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.processing.ImageFilterProject;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.utils.CommandResponse;
import org.junit.jupiter.api.Assertions;
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
public class ImageFilterProjectServiceTests {

    @Autowired
    ImageFilterProjectService imageFilterProjectService;

    @Autowired
    BasicInstanceBuilder builder;

    @Test
    public void find_image_filter_project_with_success() {
        ImageFilterProject imageFilterProject = builder.given_a_image_filter_project();
        assertThat(imageFilterProjectService.find(imageFilterProject.getImageFilter(), imageFilterProject.getProject()))
                .isPresent();
    }

    @Test
    public void find_unexisting_image_filter_project_return_empty() {
        ImageFilterProject imageFilterProject = builder.given_a_image_filter_project();
        assertThat(imageFilterProjectService.find(imageFilterProject.getImageFilter(), builder.given_a_project()))
                .isEmpty();
    }

    @Test
    public void list_all_image_filter_project() {
        ImageFilterProject imageFilterProject = builder.given_a_image_filter_project();
        assertThat(imageFilterProjectService.list()).contains(imageFilterProject);
    }

    @Test
    public void list_all_image_filter_project_by_project() {
        ImageFilterProject imageFilterProject = builder.given_a_image_filter_project();
        ImageFilterProject imageFilterProjectForAnotherProject = builder.given_a_image_filter_project();
        assertThat(imageFilterProjectService.list(imageFilterProject.getProject()))
                .contains(imageFilterProject).doesNotContain(imageFilterProjectForAnotherProject);
    }

    @Test
    public void add_valid_image_filter_project_with_success() {
        ImageFilterProject imageFilterProject =
                builder.given_a_not_persisted_image_filter_project(builder.given_a_image_filter(), builder.given_a_project());
        CommandResponse commandResponse = imageFilterProjectService.add(imageFilterProject.toJsonObject());
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }


    @Test
    public void add_already_existing_image_filter_project() {
        ImageFilterProject imageFilterProject =
                builder.given_a_not_persisted_image_filter_project(builder.given_a_image_filter(), builder.given_a_project());
        builder.persistAndReturn(imageFilterProject);

        Assertions.assertThrows(AlreadyExistException.class, () -> {
            imageFilterProjectService.add(imageFilterProject.toJsonObject());
        });
    }

    @Test
    public void add_image_filter_with_unexisting_project_return_error() {
        ImageFilterProject imageFilterProject =
                builder.given_a_not_persisted_image_filter_project(builder.given_a_image_filter(), BasicInstanceBuilder.given_a_not_persisted_project());
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            imageFilterProjectService.add(imageFilterProject.toJsonObject().withChange("project", 0L));
        });
    }

    @Test
    public void add_image_filter_with_unexisting_image_filter_return_error() {
        ImageFilterProject imageFilterProject =
                builder.given_a_not_persisted_image_filter_project(builder.given_a_not_persisted_image_filter(), builder.given_a_project());
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            imageFilterProjectService.add(imageFilterProject.toJsonObject().withChange("imageFilter", 0L));
        });
    }

    @Test
    void delete_projectRepresentativeUser_with_success() {
        ImageFilterProject imageFilterProject = builder.given_a_image_filter_project();
        CommandResponse commandResponse = imageFilterProjectService.delete(imageFilterProject, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(imageFilterProjectService.find(imageFilterProject.getImageFilter(), imageFilterProject.getProject()).isEmpty());
    }
}
