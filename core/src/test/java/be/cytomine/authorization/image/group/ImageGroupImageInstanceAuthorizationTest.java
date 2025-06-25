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
import be.cytomine.authorization.CRDAuthorizationTest;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.image.group.ImageGroupImageInstance;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.service.image.group.ImageGroupImageInstanceService;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.READ;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class ImageGroupImageInstanceAuthorizationTest extends CRDAuthorizationTest {

    private ImageGroupImageInstance igii = null;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    ImageGroupImageInstanceService imageGroupImageInstanceService;

    @BeforeEach
    public void before() throws Exception {
        if (igii == null) {
            igii = builder.given_an_imagegroup_imageinstance();
            initACL(igii.container());
        }
        ImageGroup imageGroup = igii.getGroup();
        imageGroup.getProject().setMode(EditingMode.CLASSIC);
    }

    @Override
    protected void when_i_get_domain() {
        imageGroupImageInstanceService.get(igii.getGroup(), igii.getImage());
    }

    @Override
    protected void when_i_add_domain() {
        imageGroupImageInstanceService.add(builder.given_a_not_persisted_imagegroup_imageinstance(igii.getGroup(), igii.getImage()).toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        imageGroupImageInstanceService.delete(igii, null, null, true);
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
    public void admin_can_list_imagegroup_imageinstance_by_imagegroup() {
        assertThat(imageGroupImageInstanceService.list(igii.getGroup())).contains(igii);
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_imagegroup_imageinstance_by_imageinstance() {
        assertThat(imageGroupImageInstanceService.list(igii.getImage())).contains(igii);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list_imagegroup_imageinstance_by_imagegroup() {
        assertThat(imageGroupImageInstanceService.list(igii.getGroup())).contains(igii);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list_imagegroup_imageinstance_by_imageinstance() {
        assertThat(imageGroupImageInstanceService.list(igii.getImage())).contains(igii);
    }
}
