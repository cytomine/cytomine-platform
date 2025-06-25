package be.cytomine.authorization.meta;

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
import be.cytomine.authorization.AbstractAuthorizationTest;
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.meta.Tag;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.security.User;
import be.cytomine.service.PermissionService;
import be.cytomine.service.meta.TagService;
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
public class TagAuthorizationTest extends AbstractAuthorizationTest {


    private Tag tag = null;

    @Autowired
    TagService tagService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (tag == null) {
            tag = builder.given_a_tag();
            ;
        }
    }

    @Test
    @WithMockUser(username = GUEST)
    public void everyone_can_see_a_tag_with_its_name(){
        tagService.findByName(tag.getName());
    }

    @Test
    @WithMockUser(username = GUEST)
    public void everyone_can_see_a_tag_with_its_id(){
        tagService.find(tag.getId());
    }

    @Test
    @WithMockUser(username = GUEST)
    public void everyone_can_list_tags(){
        assertThat(tagService.list()).contains(tag);
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_add_tag() {
        expectForbidden (() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_can_add_tag() {
        expectOK (() -> when_i_add_domain());
    }


    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_update_tag_even_if_linked_with_associations() {
        Tag tagToEdit = builder.given_a_tag();
        TagDomainAssociation association = builder.given_a_tag_association(tagToEdit, builder.given_a_project());
        expectOK (() -> tagService.update(tagToEdit, tagToEdit.toJsonObject()));
    }

    @Test
    @WithMockUser(username = CREATOR)
    public void creator_cannot_update_tag_if_linked_with_associations() {
        Tag tagToEdit = builder.given_a_tag();
        tagToEdit.setUser((User) userRepository.findByUsernameLikeIgnoreCase(CREATOR).get());
        builder.persistAndReturn(tagToEdit);
        TagDomainAssociation association = builder.given_a_tag_association(tagToEdit, builder.given_a_project());
        expectForbidden (() -> tagService.update(tagToEdit, tagToEdit.toJsonObject()));
    }

    @Test
    @WithMockUser(username = CREATOR)
    public void creator_can_update_tag_if_not_linked_with_associations() {
        Tag tagToEdit = builder.given_a_tag();
        tagToEdit.setUser((User) userRepository.findByUsernameLikeIgnoreCase(CREATOR).get());
        builder.persistAndReturn(tagToEdit);
        expectOK (() -> tagService.update(tagToEdit, tagToEdit.toJsonObject()));
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_tag_tag() {
        Tag tagToEdit = builder.given_a_tag();
        TagDomainAssociation association = builder.given_a_tag_association(tagToEdit, builder.given_a_project());
        expectOK (() -> tagService.delete(tagToEdit, null, null, false));
    }

    @Test
    @WithMockUser(username = CREATOR)
    public void creator_cannot_delete_tag_if_linked_with_associations() {
        Tag tagToDelete = builder.given_a_tag();
        tagToDelete.setUser((User) userRepository.findByUsernameLikeIgnoreCase(CREATOR).get());
        builder.persistAndReturn(tagToDelete);
        TagDomainAssociation association = builder.given_a_tag_association(tagToDelete, builder.given_a_project());
        expectForbidden (() -> tagService.delete(tagToDelete, null, null, false));
    }


    @Test
    @WithMockUser(username = CREATOR)
    public void creator_can_delete_tag_if_not_linked_with_associations() {
        Tag tagToDelete = builder.given_a_tag();
        tagToDelete.setUser((User) userRepository.findByUsernameLikeIgnoreCase(CREATOR).get());
        builder.persistAndReturn(tagToDelete);
        expectOK (() -> tagService.delete(tagToDelete, null, null, false));
    }


    public void when_i_get_domain() {
        tagService.get(tag.getId());
    }

    protected void when_i_add_domain() {
        tagService.add(builder.given_a_not_persisted_tag("xxx").toJsonObject());
    }

    public void when_i_edit_domain() {
        tagService.update(tag, tag.toJsonObject());
    }

    protected void when_i_delete_domain() {
        Tag tagToDelete = tag;
        tagService.delete(tagToDelete, null, null, true);
    }
}
