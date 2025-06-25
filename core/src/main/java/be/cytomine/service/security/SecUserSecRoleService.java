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

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.User;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class SecUserSecRoleService extends ModelService {


    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    SecUserSecRoleRepository secSecUserSecRoleRepository;

    @Autowired
    CurrentUserService currentUserService;

    @Autowired
    SecRoleRepository secRoleRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    public Class currentDomain() {
        return SecUserSecRoleService.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new SecUserSecRole().buildDomainFromJson(json, getEntityManager());
    }

    public List<SecUserSecRole> list(User user) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        return secSecUserSecRoleRepository.findAllBySecUser(user);
    }

    public SecRole getHighest(User user) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());

        List<SecUserSecRole> secSecUserSecRoles = secSecUserSecRoleRepository.findAllBySecUser(user);

        Optional<SecUserSecRole> role_super_admin = secSecUserSecRoles.stream()
                .filter(x -> x.getSecRole().getAuthority().equals("ROLE_SUPER_ADMIN")).findFirst();
        if (role_super_admin.isPresent()) {
            return role_super_admin.get().getSecRole();
        }

        Optional<SecUserSecRole> role_admin = secSecUserSecRoles.stream()
                .filter(x -> x.getSecRole().getAuthority().equals("ROLE_ADMIN")).findFirst();
        if (role_admin.isPresent()) {
            return role_admin.get().getSecRole();
        }

        Optional<SecUserSecRole> role_user = secSecUserSecRoles.stream()
                .filter(x -> x.getSecRole().getAuthority().equals("ROLE_USER")).findFirst();
        if (role_user.isPresent()) {
            return role_user.get().getSecRole();
        }

        Optional<SecUserSecRole> role_guest = secSecUserSecRoles.stream()
                .filter(x -> x.getSecRole().getAuthority().equals("ROLE_GUEST")).findFirst();
        if (role_guest.isPresent()) {
            return role_guest.get().getSecRole();
        }
        return null;
    }


    public Optional<SecUserSecRole> find(User user, SecRole role) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        return secSecUserSecRoleRepository.findBySecUserAndSecRole(user, role);
    }

    @Override
    public CommandResponse add(JsonObject jsonObject) {
        User currentUser = currentUserService.getCurrentUser();
        SecRole role = secRoleRepository.findById(jsonObject.getJSONAttrLong("role"))
                .orElseThrow(() -> new ObjectNotFoundException("Role", jsonObject.getJSONAttrStr("role")));
        User user = userRepository.findById(jsonObject.getJSONAttrLong("user"))
                .orElseThrow(() -> new ObjectNotFoundException("User", jsonObject.getJSONAttrStr("user")));
        Set<String> userRoles = secSecUserSecRoleRepository.findAllBySecUser(user).stream().map(x -> x.getSecRole().getAuthority())
                .collect(Collectors.toSet());

        if (role.getAuthority().equals("ROLE_ADMIN") || role.getAuthority().equals("ROLE_SUPER_ADMIN")) {
            securityACLService.checkAdmin(currentUser);
        } else if (userRoles.size()==1 && userRoles.contains("ROLE_GUEST")) {
            securityACLService.checkAdmin(currentUser);
        }else {
            securityACLService.checkUser(currentUser);
        }




        return executeCommand(new AddCommand(currentUser),null,jsonObject);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        SecUserSecRole secSecUserSecRole = (SecUserSecRole)domain;
        if(Objects.equals(secSecUserSecRole.getSecUser().getId(), currentUser.getId()) && !secSecUserSecRole.getSecRole().getAuthority().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("You cannot remove you a role");
        }
        securityACLService.checkAdmin(currentUser);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }



    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        SecUserSecRole secSecUserSecRole = (SecUserSecRole)domain;
        return List.of(secSecUserSecRole.getSecUser().getId(), secSecUserSecRole.getSecRole().getId());
    }


    /**
     * Define a role for a user. If admin is defined, user will have admin,user,guest. If user is defined, user will have user,guest, etc
     */
    public void define(User user, SecRole role) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkAdmin(currentUser);

        SecRole roleGuest = secRoleRepository.getByAuthority("ROLE_GUEST");
        SecRole roleUser = secRoleRepository.getByAuthority("ROLE_USER");
        SecRole roleAdmin = secRoleRepository.getByAuthority("ROLE_ADMIN");
        SecRole roleSuperAdmin = secRoleRepository.getByAuthority("ROLE_SUPER_ADMIN");

        switch (role.getAuthority()) {
            case "ROLE_SUPER_ADMIN":
                addRole(user, roleGuest);
                addRole(user, roleUser);
                addRole(user, roleAdmin);
                addRole(user, roleSuperAdmin);
                break;
            case "ROLE_ADMIN":
                addRole(user, roleGuest);
                addRole(user, roleUser);
                addRole(user, roleAdmin);
                removeRole(user, roleSuperAdmin);
                break;
            case "ROLE_USER":
                addRole(user, roleGuest);
                addRole(user, roleUser);
                removeRole(user, roleAdmin);
                removeRole(user, roleSuperAdmin);
                break;
            case "ROLE_GUEST":
                addRole(user, roleGuest);
                removeRole(user, roleUser);
                removeRole(user, roleAdmin);
                removeRole(user, roleSuperAdmin);
                break;
        }
    }


    private void addRole(User user,SecRole role) {
        Optional<SecUserSecRole> linked = secSecUserSecRoleRepository.findBySecUserAndSecRole(user,role);
        if(linked.isEmpty()) {
            SecUserSecRole susr = new SecUserSecRole();
            susr.setSecRole(role);
            susr.setSecUser(user);
            super.saveDomain(susr);
        }
    }
    private void removeRole(User user,SecRole role) {
        Optional<SecUserSecRole> linked = secSecUserSecRoleRepository.findBySecUserAndSecRole(user,role);
        if(linked.isPresent()) {
            if(Objects.equals(user.getId(), currentUserService.getCurrentUser().getId()) && !role.getAuthority().equals("ROLE_SUPER_ADMIN")) {
                throw new ForbiddenException("You cannot remove you a role");
            }
            super.removeDomain(linked.get());
        }
    }

    /**
     * Retrieve domain thanks to a JSON object
     * @return domain retrieve thanks to json
     */
    @Override
    public CytomineDomain retrieve(JsonObject json) {

       User secUser = userRepository.getById(json.getJSONAttrLong("user"));
       SecRole secRole = secRoleRepository.getById(json.getJSONAttrLong("role"));
       return secSecUserSecRoleRepository.findBySecUserAndSecRole(secUser,secRole)
               .orElseThrow(() -> new ObjectNotFoundException("SecUserSecRole", json.toJsonString()));
    }


    public void checkDoNotAlreadyExist(CytomineDomain domain){
        SecUserSecRole secSecUserSecRole = (SecUserSecRole)domain;
        if(domain!=null) {
            if(secSecUserSecRoleRepository.findBySecUserAndSecRole(secSecUserSecRole.getSecUser(), secSecUserSecRole.getSecRole())
                    .stream().anyMatch(x -> !Objects.equals(x.getId(), secSecUserSecRole.getId())))  {
                throw new AlreadyExistException("User " + secSecUserSecRole.getSecUser().getUsername() + " has already role " + secSecUserSecRole.getSecRole().getAuthority());
            }
        }
    }

}
