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

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.PermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public abstract class AbstractAuthorizationTest {

    public static final String SUPERADMIN = "SUPER_ADMIN_ACL";

    public static final String ADMIN = "ADMIN_ACL";

    public static final String GUEST = "GUEST_ACL";

    public static final String USER_ACL_READ = "USER_ACL_READ";

    public static final String USER_ACL_CREATE = "USER_ACL_CREATE";

    public static final String USER_ACL_WRITE = "USER_ACL_WRITE";

    public static final String USER_ACL_DELETE = "USER_ACL_DELETE";

    public static final String USER_ACL_ADMIN = "USER_ACL_ADMIN";

    public static final String USER_NO_ACL = "ACL_USER_NO_ACL";

    public static final String CREATOR = "CREATOR";

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected SecUserSecRoleRepository secSecUserSecRoleRepository;

    @Autowired
    protected SecRoleRepository secRoleRepository;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected CurrentUserService currentUserService;

    protected void initACL(CytomineDomain container) {

        // This code is called every execution of an authorization (a lot...)
        // So it is a "ugly" implementation of the 'addPermission' that is very fast.

        User user = (User) currentUserService.getCurrentUser();

        Long aclClassId = permissionService.getAclClassId(container);
        //get acl sid for current user (run request)
        Long sidCurrentUser = permissionService.getAclSid(user.getUsername());
        //get acl object id
        Long aclObjectIdentity = permissionService.getAclObjectIdentity(container, aclClassId, sidCurrentUser);

        permissionService.addPermissionOptimised(aclObjectIdentity, USER_ACL_READ, BasePermission.READ,100);
        permissionService.addPermissionOptimised(aclObjectIdentity, USER_ACL_WRITE, BasePermission.WRITE,101);
        permissionService.addPermissionOptimised(aclObjectIdentity, USER_ACL_CREATE, BasePermission.CREATE,102);
        permissionService.addPermissionOptimised(aclObjectIdentity, USER_ACL_DELETE, BasePermission.DELETE,103);
        permissionService.addPermissionOptimised(aclObjectIdentity, USER_ACL_ADMIN, BasePermission.ADMINISTRATION,104);
        permissionService.addPermissionOptimised(aclObjectIdentity, GUEST, BasePermission.READ,105);
        permissionService.addPermissionOptimised(aclObjectIdentity, CREATOR, BasePermission.CREATE,106);
    }

    protected void expectForbidden(Executable executable) {
        Assertions.assertThrows(ForbiddenException.class, executable) ;
    }

    protected void expectOK(Executable executable) {
        Assertions.assertDoesNotThrow(executable);
    }


}
