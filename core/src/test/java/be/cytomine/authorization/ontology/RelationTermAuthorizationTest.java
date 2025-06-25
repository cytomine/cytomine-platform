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
import be.cytomine.authorization.CRDAuthorizationTest;
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.RelationTermService;
import be.cytomine.service.ontology.TermService;
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

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class RelationTermAuthorizationTest extends CRDAuthorizationTest {


    private RelationTerm relationTerm = null;

    @Autowired
    RelationTermService relationTermService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (relationTerm == null) {
            relationTerm = builder.given_a_relation_term();
            ;
            initACL(relationTerm.container());
        }
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_relation_terms() {
        expectOK (() -> { relationTermService.list(relationTerm.getTerm1()); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_can_list_relation_terms(){
        expectOK(() -> {
            relationTermService.list(relationTerm.getTerm1());
        });
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_no_acl_cannot_list_relation_terms(){
        expectForbidden(() -> {
            relationTermService.list(relationTerm.getTerm1());
        });
    }


    @Override
    public void when_i_get_domain() {
        relationTermService.find(relationTerm.getRelation(), relationTerm.getTerm1(), relationTerm.getTerm2());
    }

    @Override
    protected void when_i_add_domain() {
        relationTermService.add(
                BasicInstanceBuilder.given_a_not_persisted_relation_term(relationTerm.getRelation(), relationTerm.getTerm1(), builder.given_a_term(relationTerm.getTerm1().getOntology())).toJsonObject()
        );
    }

    @Override
    protected void when_i_delete_domain() {
        RelationTerm termToDelete = builder.given_a_relation_term(relationTerm.getRelation(), relationTerm.getTerm1(), builder.given_a_term(relationTerm.getTerm1().getOntology()));
        relationTermService.delete(termToDelete, null, null, true);
    }
    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(BasePermission.WRITE);
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
