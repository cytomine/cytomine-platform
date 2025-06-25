package be.cytomine.service;

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
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.AclRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
@Transactional
public class PermissionService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AclRepository aclRepository;

    public boolean hasACLPermission(CytomineDomain domain, String username, Permission permission) {
        List<Integer> masks = getPermissionInACL(domain,username);
        return masks.stream().max(Integer::compare).orElse(-1) >= permission.getMask();
    }

    public boolean hasExactACLPermission(CytomineDomain domain, String username, Permission permission) {
        List<Integer> masks = getPermissionInACL(domain,username);
        return masks.contains(permission.getMask());
    }

    public boolean hasACLPermission(CytomineDomain domain, Permission permission) {
        return hasACLPermission(domain, currentUserService.getCurrentUsername(), permission);
    }

    public boolean hasACLPermission(CytomineDomain domain, Permission permission, boolean isAdmin) {
        return isAdmin || hasACLPermission(domain, currentUserService.getCurrentUsername(), permission);
    }

    List<Integer> getPermissionInACL(CytomineDomain domain, User user) {
        return aclRepository.listMaskForUsers(domain.getId(), user.getUsername());
    }

    List<Integer> getPermissionInACL(CytomineDomain domain, String username) {
        return aclRepository.listMaskForUsers(domain.getId(), username);
    }


    public void deletePermission(CytomineDomain domain, String username, Permission permission) {
        log.debug("Current mask for user {} on domain {} before request: {}", username, domain.getId(), aclRepository.listMaskForUsers(domain.getId(), username));
        if (hasACLPermission(domain, username, permission)) {
            log.info("Delete permission for {}, {}, {}", username, permission.getMask(), domain.getId());

            Long aclObjectIdentity = aclRepository.getAclObjectIdentityFromDomainId(domain.getId());
            int mask = permission.getMask();
            Long sid = aclRepository.getAclSid(username);

            if(aclObjectIdentity==null || sid==null) {
                throw new ObjectNotFoundException("User " + username + " or Object " + domain.getId() + " are not in ACL");
            }
            aclRepository.deleteAclEntry(aclObjectIdentity, mask, sid);

            log.info("User " + username + " right " + permission.getMask() + " in domain " + domain + " => " + hasACLPermission(domain, username, permission));
        }
        log.debug("Current mask for user {} on domain {} after request: {}", username, domain.getId(), aclRepository.listMaskForUsers(domain.getId(), username));
    }

    /**
     * Add Permission right
     * @param domain
     * @param username
     * @param permission
     */
    public void addPermission(CytomineDomain domain, String username, int permission) {
        addPermission(domain, username, readFromMask(permission));
    }

    public void addPermission(CytomineDomain domain, String username, Permission permission) {
        addPermission(domain,username,permission,currentUserService.getCurrentUser());
    }

    public void addPermission(CytomineDomain domain, String username, Permission permission, User user) {
        if (!hasExactACLPermission(domain, username, permission)) {
            //get domain class id
            Long aclClassId = getAclClassId(domain);

            //get acl sid for current user (run request)
            Long sidCurrentUser = getAclSid(user.getUsername());

            //get acl object id
            Long aclObjectIdentity = getAclObjectIdentity(domain, aclClassId, sidCurrentUser);

            //get acl sid for the user
            Long sid = getAclSid(username);

            //get acl entry
            createAclEntry(aclObjectIdentity, sid, permission.getMask());
        }
    }


    public void addPermissionOptimised(Long aclObjectIdentity, String username, Permission permission, Integer index) {
        //get acl sid for the user
        Long sid = getAclSid(username);
        aclRepository.insertAclEntry(index, aclObjectIdentity, permission.getMask(), sid);
    }



    public Long createAclEntry(Long aoi, Long sid, Integer mask) {
        log.debug("create acl entry for {}, {}, {}", aoi, sid, mask);
        synchronized (this.getClass()) {
            // method is synchronized since we have to compute the aceOrder+1.
            // the synchronize does not resolve fully the issue since the INSERT may not be persisted in database directly (batch_insert / flush / ...)
            // TODO: Not sure the aceOrder is really usefull ? couldn't we use a sequence?
            Long aclEntryId = aclRepository.getAclEntryId(aoi, sid, mask);
            if (aclEntryId == null) {
                Integer max = aclRepository.getMaxAceOrder(aoi);
                if(max==null) {
                    max=0;
                } else {
                    max = max+new Random().nextInt(25)+1;
                }
                log.debug("next ace order {} for {}", max, aoi);
                aclRepository.insertAclEntry(max, aoi, mask, sid);
                aclEntryId = aclRepository.getAclEntryId(aoi, sid, mask);
            }
            return aclEntryId;
        }
    }

    public Long getAclObjectIdentity(CytomineDomain domain, Long aclClassId, Long aclSidId) {
        Long aclObjectIdentityId = aclRepository.getAclObjectIdentityFromDomainId(domain.getId());
        if (aclObjectIdentityId == null) {
            aclRepository.insertAclObjectIdentity(aclClassId, domain.getId(), aclSidId);
            aclObjectIdentityId = aclRepository.getAclObjectIdentityFromDomainId(domain.getId());
        }
        return aclObjectIdentityId;
    }

    public Long getAclSid(String username) {
        Long id = aclRepository.getAclSidFromUsername(username);
        if (id == null) {
            aclRepository.insertAclSid(username);
            id = aclRepository.getAclSidFromUsername(username);
        }
        return id;
    }

    public Long getAclClassId(CytomineDomain domain) {
        Long id = aclRepository.getAclClassId(domain.getClass().getName());
        if (id == null) {
            aclRepository.insertAclClassId(domain.getClass().getName());
            id = aclRepository.getAclClassId(domain.getClass().getName());
        }
        return id;
    }

    Permission readFromMask(int mask) {
        switch (mask) {
            case 1:
                return BasePermission.READ;
            case 2:
                return BasePermission.WRITE;
            case 4:
                return BasePermission.CREATE;
            case 8:
                return BasePermission.DELETE;
            case 16:
                return BasePermission.ADMINISTRATION;
        }
        throw new RuntimeException("Mask " + mask + " not supported");
    }
}
