package be.cytomine.service.ontology;

import jakarta.transaction.Transactional;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.ontology.AnnotationLink;
import be.cytomine.domain.project.Project;
import be.cytomine.utils.CommandResponse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class AnnotationLinkServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    AnnotationLinkService annotationLinkService;

    @Test
    void find_annotation_link_with_success() {
        AnnotationLink annotationLink = builder.given_an_annotation_link();
        AssertionsForClassTypes.assertThat(annotationLinkService.find(annotationLink.getId()).isPresent());
        assertThat(annotationLink).isEqualTo(annotationLinkService.find(annotationLink.getId()).get());
    }

    @Test
    void find_non_existing_annotation_link_return_empty() {
        AssertionsForClassTypes.assertThat(annotationLinkService.find(0L)).isEmpty();
    }

    @Test
    void get_non_existing_annotation_link_return_null() {
        AssertionsForClassTypes.assertThat(annotationLinkService.get(0L)).isNull();
    }

    @Test
    void list_annotation_link_by_annotation_group() {
        Project project = builder.given_a_project();
        AnnotationGroup annotationGroup = builder.given_an_annotation_group(project, builder.given_an_imagegroup(project));
        ImageInstance image = builder.given_an_image_instance(project);

        AnnotationLink annotationLink1 = builder.given_an_annotation_link(builder.given_a_user_annotation(project), annotationGroup, image);
        AnnotationLink annotationLink2 = builder.given_an_annotation_link(builder.given_a_user_annotation(project), annotationGroup, image);
        AnnotationLink annotationLink3 = builder.given_an_annotation_link(builder.given_a_user_annotation(project), annotationGroup, image);
        AnnotationLink annotationLink4 = builder.given_an_annotation_link();

        AssertionsForInterfaceTypes.assertThat(annotationLinkService.list(annotationGroup)).containsExactly(annotationLink1, annotationLink2, annotationLink3);
        AssertionsForInterfaceTypes.assertThat(annotationLinkService.list(annotationGroup)).doesNotContain(annotationLink4);
    }

    @Test
    void add_valid_annotation_link_with_success() {
        AnnotationLink annotationLink = builder.given_a_not_persisted_annotation_link();

        CommandResponse commandResponse = annotationLinkService.add(annotationLink.toJsonObject());

        AssertionsForClassTypes.assertThat(commandResponse).isNotNull();
        AssertionsForClassTypes.assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(annotationLinkService.find(commandResponse.getObject().getId())).isPresent();
    }

    @Test
    void delete_annotation_link_with_success() {
        AnnotationLink annotationLink = builder.given_an_annotation_link();

        CommandResponse commandResponse = annotationLinkService.delete(annotationLink, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(annotationLinkService.find(annotationLink.getId()).isEmpty());
    }
}
