package be.cytomine.authorization.image.group;

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
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.service.image.group.ImageGroupService;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.READ;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class ImageGroupAuthorizationTest extends CRUDAuthorizationTest {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private ImageGroupService imageGroupService;

    private ImageGroup imageGroup = null;

    @BeforeEach
    public void before() throws Exception {
        if (imageGroup == null) {
            imageGroup = builder.given_an_imagegroup();
            initACL(imageGroup.container());
        }
        imageGroup.getProject().setMode(EditingMode.CLASSIC);
        imageGroup.getProject().setAreImagesDownloadable(true);
    }

    @Override
    protected void when_i_get_domain() {
        imageGroupService.get(imageGroup.getId());
    }

    @Override
    protected void when_i_add_domain() {
        imageGroupService.add(builder.given_a_not_persisted_imagegroup(imageGroup.getProject()).toJsonObject());
    }

    @Override
    protected void when_i_edit_domain() {
        imageGroupService.update(imageGroup, imageGroup.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        ImageGroup imageGroupToDelete = imageGroup;
        imageGroupService.delete(imageGroupToDelete, null, null, true);
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
    public void admin_can_list_imagegroup() {
        assertThat(imageGroupService.list(imageGroup.getProject())).contains(imageGroup);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list_imagegroup() {
        assertThat(imageGroupService.list(imageGroup.getProject())).contains(imageGroup);
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_admin_can_add_in_readonly_mode(){
        imageGroup.getProject().setMode(EditingMode.READ_ONLY);
        expectOK(() -> when_i_add_domain());
    }
}
