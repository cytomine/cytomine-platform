package be.cytomine.authorization;

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

import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

@Transactional
public abstract class CRUDAuthorizationTest extends CRDAuthorizationTest {


    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_edit_domain() {
        if (minimalRoleForEdit().isPresent() && minimalRoleForEdit().get().equals("CREATOR")) {
            expectOK (() -> when_i_edit_domain());
        } else if (isPermissionRoleForbidden(minimalRoleForEdit(), "ROLE_SUPERADMIN")) {
            expectForbidden (() -> when_i_edit_domain());
        } else {
            expectOK (() -> when_i_edit_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_permission_edit_domain() {
        if (minimalRoleForEdit().isPresent() && minimalRoleForEdit().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_edit_domain());
        } else if (isPermissionForbidden(minimalPermissionForEdit(), BasePermission.ADMINISTRATION)) {
            expectForbidden (() -> when_i_edit_domain());
        } else {
            expectOK (() -> when_i_edit_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_DELETE)
    public void user_with_delete_permission_edit_domain() {
        if (minimalRoleForEdit().isPresent() && minimalRoleForEdit().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_edit_domain());
        } else if (isPermissionForbidden(minimalPermissionForEdit(), BasePermission.DELETE)) {
            expectForbidden (() -> when_i_edit_domain());
        } else {
            expectOK (() -> when_i_edit_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_CREATE)
    public void user_with_create_permission_edit_domain() {
        if (minimalRoleForEdit().isPresent() && minimalRoleForEdit().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_edit_domain());
        } else if (isPermissionForbidden(minimalPermissionForEdit(), BasePermission.CREATE)) {
            expectForbidden (() -> when_i_edit_domain());
        } else {
            expectOK (() -> when_i_edit_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_permission_edit_domain() {
        if (minimalRoleForEdit().isPresent() && minimalRoleForEdit().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_edit_domain());
        } else if (isPermissionForbidden(minimalPermissionForEdit(), BasePermission.WRITE)) {
            expectForbidden (() -> when_i_edit_domain());
        } else {
            expectOK (() -> when_i_edit_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_edit_domain() {
        if (minimalRoleForEdit().isPresent() && minimalRoleForEdit().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_edit_domain());
        } else if (isPermissionForbidden(minimalPermissionForEdit(), BasePermission.READ)) {
            expectForbidden (() -> when_i_edit_domain());
        } else {
            expectOK (() -> when_i_edit_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_edit_domain() {
        if (minimalRoleForEdit().isPresent() && minimalRoleForEdit().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_edit_domain());
        } else if (isPermissionForbidden(minimalPermissionForEdit(), null)) {
            expectForbidden (() -> when_i_edit_domain());
        } else {
            expectOK (() -> when_i_edit_domain());
        }
    }

   @Test
    @WithMockUser(username = GUEST)
    public void guest_edit_domain() {
        if (minimalRoleForEdit().isPresent() && minimalRoleForEdit().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_edit_domain());
        } else if (isPermissionRoleForbidden(minimalRoleForEdit(), "ROLE_GUEST")) {
            expectForbidden (() -> when_i_edit_domain());
        } else {
            expectOK (() -> when_i_edit_domain());
        }
    }


    protected abstract void when_i_edit_domain();
}
