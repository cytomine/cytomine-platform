package be.cytomine.repository.security;

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

import be.cytomine.domain.security.SecRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface SecRoleRepository extends JpaRepository<SecRole, Long>, JpaSpecificationExecutor<SecRole> {

    String ROLE_GUEST = "ROLE_GUEST";
    String ROLE_USER = "ROLE_USER";
    String ROLE_ADMIN = "ROLE_ADMIN";
    String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    default SecRole getGuest() {
        return getByAuthority(ROLE_GUEST);
    }

    default SecRole getUser() {
        return getByAuthority(ROLE_USER);
    }

    default SecRole getAdmin() {
        return getByAuthority(ROLE_ADMIN);
    }

    default SecRole getSuperAdmin() {
        return getByAuthority(ROLE_SUPER_ADMIN);
    }

    SecRole getByAuthority(String authority);

    Optional<SecRole> findByAuthority(String authority);

    default SecRole createIfNotExist(String authority) {
        return findByAuthority(authority).orElseGet(() -> {
            SecRole role = new SecRole();
            role.setAuthority(authority);
            return save(role);
        });
    }

}
