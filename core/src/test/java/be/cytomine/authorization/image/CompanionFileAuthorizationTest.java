package be.cytomine.authorization.image;

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
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.CompanionFile;
import be.cytomine.service.PermissionService;
import be.cytomine.service.image.AbstractImageService;
import be.cytomine.service.image.CompanionFileService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class CompanionFileAuthorizationTest extends CRUDAuthorizationTest {

    private CompanionFile companionFile = null;

    @Autowired
    CompanionFileService companionFileService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (companionFile == null) {
            companionFile = builder.given_a_companion_file(builder.given_an_abstract_image());
            initACL(companionFile.container());
        }
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list() {
        assertThat(companionFileService.list(companionFile.getImage())).contains(companionFile);
        AbstractImage anotherAbstractImage = builder.given_an_abstract_image();
        assertThat(companionFileService.list(companionFile.getUploadedFile())).contains(companionFile);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list(){
        assertThat(companionFileService.list(companionFile.getImage())).contains(companionFile);
        AbstractImage anotherAbstractImage = builder.given_an_abstract_image();
        assertThat(companionFileService.list(companionFile.getUploadedFile())).contains(companionFile);
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_with_no_read_cannot_list(){
        expectForbidden(() -> companionFileService.list(companionFile.getImage()));
        expectForbidden(() -> companionFileService.list(companionFile.getUploadedFile()));
    }


    @Override
    public void when_i_get_domain() {
        companionFileService.get(companionFile.getId());
    }

    @Override
    protected void when_i_add_domain() {
        AbstractImage abstractImage = builder.given_an_abstract_image();
        abstractImage.getUploadedFile().setStorage(companionFile.getImage().getUploadedFile().getStorage());
        companionFileService.add(builder.given_a_not_persisted_companion_file(abstractImage).toJsonObject());
    }

    @Override
    public void when_i_edit_domain() {
        companionFileService.update(companionFile, companionFile.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        CompanionFile companionFileToDelete = companionFile;
        companionFileService.delete(companionFileToDelete, null, null, true);
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.empty();
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(WRITE);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(WRITE);
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
}
