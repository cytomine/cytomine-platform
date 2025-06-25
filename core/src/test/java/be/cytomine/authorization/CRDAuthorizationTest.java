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
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Transactional
public abstract class CRDAuthorizationTest extends AbstractAuthorizationTest {

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_permission_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_DELETE)
    public void user_with_delete_permission_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_CREATE)
    public void user_with_create_permission_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_permission_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_get_domain() {
        expectForbidden (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_with_permission_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_add_domain() {
        expectOK (() -> when_i_add_domain());
    }


    protected static List<String> rolePerOrder = List.of("ROLE_GUEST", "ROLE_USER", "CREATOR", "ROLE_ADMIN", "ROLE_SUPERADMIN");
    // CREATOR is a special case for domain with restriction for users other than creator (only a creator can modify its reviewed annotation)

    boolean isPermissionForbidden(Optional<Permission> permissionRequired, Permission permission) {
        return permissionRequired.isPresent() && (permission==null || permissionRequired.get().getMask() > permission.getMask());
    }
    boolean isPermissionRoleForbidden(Optional<String> roleRequired, String currentRole) {
        if (roleRequired.isEmpty()) {
            return false;
        } else {
            int indexRoleRequired = rolePerOrder.indexOf(roleRequired.get());
            int indexCurrenRole = rolePerOrder.indexOf(currentRole);
            if (indexRoleRequired==-1 || indexCurrenRole==-1) {
                throw new RuntimeException("Cannot find index for role " + roleRequired.get() + " or " + currentRole);
            }
            return indexCurrenRole < indexRoleRequired;
        }

    }


    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_permission_add_domain() {
        if (isPermissionForbidden(minimalPermissionForCreate(), BasePermission.ADMINISTRATION)) {
            expectForbidden (() -> when_i_add_domain());
        } else {
            expectOK (() -> when_i_add_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_DELETE)
    public void user_with_delete_permission_add_domain() {
        if (isPermissionForbidden(minimalPermissionForCreate(), BasePermission.DELETE)) {
            expectForbidden (() -> when_i_add_domain());
        } else {
            expectOK (() -> when_i_add_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_permission_add_domain() {
        if (isPermissionForbidden(minimalPermissionForCreate(), BasePermission.WRITE)) {
            expectForbidden (() -> when_i_add_domain());
        } else {
            expectOK (() -> when_i_add_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_CREATE)
    public void user_with_create_permission_add_domain() {
        if (isPermissionForbidden(minimalPermissionForCreate(), BasePermission.CREATE)) {
            expectForbidden (() -> when_i_add_domain());
        } else {
            expectOK (() -> when_i_add_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_add_domain() {
        if (isPermissionForbidden(minimalPermissionForCreate(), BasePermission.READ)) {
            expectForbidden (() -> when_i_add_domain());
        } else {
            expectOK (() -> when_i_add_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_add_domain() {
        if (isPermissionForbidden(minimalPermissionForCreate(), null)) {
            expectForbidden (() -> when_i_add_domain());
        } else {
            expectOK (() -> when_i_add_domain());
        }
    }


    @Test
    @WithMockUser(username = GUEST)
    public void guest_add_domain() {
        if (isPermissionRoleForbidden(minimalRoleForCreate(), "ROLE_GUEST")) {
            expectForbidden (() -> when_i_add_domain());
        } else {
            expectOK (() -> when_i_add_domain());
        }
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_delete_domain() {
        if (minimalRoleForDelete().isPresent() && minimalRoleForDelete().get().equals("CREATOR")) {
            expectOK (() -> when_i_delete_domain());
        } else if (isPermissionRoleForbidden(minimalRoleForDelete(), "ROLE_SUPERADMIN")) {
            expectForbidden (() -> when_i_delete_domain());
        } else {
            expectOK (() -> when_i_delete_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_permission_delete_domain() {
        if (minimalRoleForDelete().isPresent() && minimalRoleForDelete().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_delete_domain());
        } else if (isPermissionForbidden(minimalPermissionForDelete(), BasePermission.ADMINISTRATION)) {
            expectForbidden (() -> when_i_delete_domain());
        } else {
            expectOK (() -> when_i_delete_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_DELETE)
    public void user_with_delete_permission_delete_domain() {
        if (minimalRoleForDelete().isPresent() && minimalRoleForDelete().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_delete_domain());
        } else if (isPermissionForbidden(minimalPermissionForDelete(), BasePermission.DELETE)) {
            expectForbidden (() -> when_i_delete_domain());
        } else {
            expectOK (() -> when_i_delete_domain());
        }
    }


    @Test
    @WithMockUser(username = USER_ACL_CREATE)
    public void user_with_create_permission_delete_domain() {
        if (minimalRoleForDelete().isPresent() && minimalRoleForDelete().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_delete_domain());
        } else if (isPermissionForbidden(minimalPermissionForDelete(), BasePermission.CREATE)) {
            expectForbidden (() -> when_i_delete_domain());
        } else {
            expectOK (() -> when_i_delete_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_permission_delete_domain() {
        if (minimalRoleForDelete().isPresent() && minimalRoleForDelete().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_delete_domain());
        } else if (isPermissionForbidden(minimalPermissionForDelete(), BasePermission.WRITE)) {
            expectForbidden (() -> when_i_delete_domain());
        } else {
            expectOK (() -> when_i_delete_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_delete_domain() {
        if (minimalRoleForDelete().isPresent() && minimalRoleForDelete().get().equals("CREATOR")) {
            expectForbidden (() -> when_i_delete_domain());
        } else if (isPermissionForbidden(minimalPermissionForDelete(), BasePermission.READ)) {
            expectForbidden (() -> when_i_delete_domain());
        } else {
            expectOK (() -> when_i_delete_domain());
        }
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_delete_domain() {
        if (isPermissionForbidden(minimalPermissionForDelete(), null)) {
            expectForbidden (() -> when_i_delete_domain());
        } else {
            expectOK (() -> when_i_delete_domain());
        }
    }


   @Test
    @WithMockUser(username = GUEST)
    public void guest_delete_domain() {
        if (isPermissionRoleForbidden(minimalRoleForDelete(), "ROLE_GUEST")) {
            expectForbidden (() -> when_i_delete_domain());
        } else {
            expectOK (() -> when_i_delete_domain());
        }
    }


    protected abstract void when_i_get_domain();

    protected abstract void when_i_add_domain();

    protected abstract void when_i_delete_domain();

    protected abstract Optional<Permission> minimalPermissionForCreate();

    protected abstract Optional<Permission> minimalPermissionForDelete();

    protected abstract Optional<Permission> minimalPermissionForEdit();

    protected abstract Optional<String> minimalRoleForCreate();

    protected abstract Optional<String> minimalRoleForDelete();

    protected abstract Optional<String> minimalRoleForEdit();
}
