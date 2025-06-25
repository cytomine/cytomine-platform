package be.cytomine.service.security;

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
import be.cytomine.repository.security.SecRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class SecRoleService {

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    SecRoleRepository secRoleRepository;

    public Optional<SecRole> find(Long id) {
        securityACLService.checkGuest();
        return secRoleRepository.findById(id);
    }

    public Optional<SecRole> findByAuthority(String authority) {
        securityACLService.checkGuest();
        return secRoleRepository.findByAuthority(authority);
    }

    public Optional<SecRole> findByTokenAuthority(String authority) {
        return secRoleRepository.findByAuthority(authority);
    }

    public List<SecRole> list() {
        securityACLService.checkGuest();
        return secRoleRepository.findAll();
    }
}
