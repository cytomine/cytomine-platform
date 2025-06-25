package be.cytomine.authorization.ontology;

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
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class OntologyAuthorizationTest extends CRUDAuthorizationTest {


    private Ontology ontology = null;

    @Autowired
    OntologyService ontologyService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (ontology == null) {
            ontology = builder.given_an_ontology();
            ;
            initACL(ontology);
        }
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_ontologies() {
        assertThat(ontologyService.list()).contains(ontology);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_at_least_read_permission_can_list_ontologies(){
        assertThat(ontologyService.list()).contains(ontology);
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_no_acl_cannot_list_ontologies(){
        assertThat(ontologyService.list()).doesNotContain(ontology);
    }

    @Override
    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_add_domain() {
        expectOK (() -> when_i_add_domain());
        // User with no ACL can create an ontology
    }
    @Override
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_add_domain() {
        expectOK (() -> when_i_add_domain());
        // User with READ permission can create another ontology
    }

    @Override
    public void when_i_get_domain() {
        ontologyService.get(ontology.getId());
    }

    @Override
    protected void when_i_add_domain() {
        ontologyService.add(BasicInstanceBuilder.given_a_not_persisted_ontology().toJsonObject());
    }

    @Override
    public void when_i_edit_domain() {
        ontologyService.update(ontology, ontology.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        Ontology ontologyToDelete = ontology;
        ontologyService.delete(ontologyToDelete, null, null, true);
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.empty();
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(BasePermission.DELETE);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(BasePermission.WRITE);
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
