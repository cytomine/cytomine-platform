package be.cytomine.authorization.ontology;

import java.util.Optional;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.authorization.CRDAuthorizationTest;
import be.cytomine.domain.ontology.AnnotationLink;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.service.ontology.AnnotationLinkService;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.READ;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class AnnotationLinkAuthorizationTest extends CRDAuthorizationTest {

    private AnnotationLink annotationLink = null;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    AnnotationLinkService annotationLinkService;

    @BeforeEach
    public void before() throws Exception {
        if (annotationLink == null) {
            annotationLink = builder.given_an_annotation_link();
            initACL(annotationLink.container());
        }
        annotationLink.getGroup().getProject().setMode(EditingMode.CLASSIC);
    }

    @Override
    protected void when_i_get_domain() {
        annotationLinkService.get(annotationLink.getId());
    }

    @Override
    protected void when_i_add_domain() {
        UserAnnotation annotation = builder.given_a_user_annotation();
        annotation.setImage(annotationLink.getImage());
        annotation.setProject(annotationLink.getImage().getProject());

        annotationLinkService.add(builder.given_a_not_persisted_annotation_link(
                annotation, annotationLink.getGroup(), annotation.getImage()
        ).toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        annotationLinkService.delete(annotationLink, null, null, true);
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(READ);
    }

    @Override
    protected Optional<String> minimalRoleForCreate() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForDelete() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForEdit() {
        return Optional.of("ROLE_USER");
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_annotation_group_by_annotation_group() {
        assertThat(annotationLinkService.list(annotationLink.getGroup())).contains(annotationLink);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list_annotation_link_by_annotation_group() {
        assertThat(annotationLinkService.list(annotationLink.getGroup())).contains(annotationLink);
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_add_in_readonly_mode(){
        annotationLink.getImage().getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_update_annotation_group_in_restricted_project() {
        AnnotationLink annotationLink = builder.given_an_annotation_link();
        annotationLink.getImage().getProject().setMode(EditingMode.RESTRICTED);
        expectOK (() -> { when_i_get_domain(); });
        expectOK (() -> { when_i_add_domain(); });
        expectOK (() -> { when_i_delete_domain(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_update_annotation_group_in_classic_project() {
        AnnotationLink annotationLink = builder.given_an_annotation_link();
        annotationLink.getImage().getProject().setMode(EditingMode.CLASSIC);
        expectOK (() -> { when_i_get_domain(); });
        expectOK (() -> { when_i_add_domain(); });
        expectOK (() -> { when_i_delete_domain(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_delete_in_readonly_mode(){
        annotationLink.getImage().getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_delete_domain());
    }
}
