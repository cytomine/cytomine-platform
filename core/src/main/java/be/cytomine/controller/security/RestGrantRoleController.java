package be.cytomine.controller.security;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.dto.auth.AuthInformation;
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

import be.cytomine.domain.security.User;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

// TODO IAM - ADMIN SESSION ADAPT IF NEEDED
@RestController
@RequestMapping("")
@Slf4j
@RequiredArgsConstructor
public class RestGrantRoleController extends RestCytomineController {

    private final CurrentRoleService currentRoleService;

    private final CurrentUserService currentUserService;

    @GetMapping("/session/admin/open.json")
    public ResponseEntity<String> openAdminSession() {
        HttpSession session = request.getSession();
        log.debug("REST request to open admin session: " + session.getId());
        currentRoleService.activeAdminSession(currentUserService.getCurrentUser());
        return responseSuccess(JsonObject.toJsonString(getCurrentRole()));
    }

    @GetMapping("/session/admin/close.json")
    public ResponseEntity<String> closeAdminSession() {
        HttpSession session = request.getSession();
        log.debug("REST request to close admin session: " + session.getId());
        currentRoleService.closeAdminSession(currentUserService.getCurrentUser());
        return responseSuccess(JsonObject.toJsonString(getCurrentRole()));
    }

    @GetMapping("/session/admin/info.json")
    public ResponseEntity<String> infoAdminSession() {
        HttpSession session = request.getSession();
        log.debug("REST request to get info from admin session: " + session.getId());
        return responseSuccess(JsonObject.toJsonString(getCurrentRole()));
    }


    public AuthInformation getCurrentRole() {
        User user = currentUserService.getCurrentUser();
        AuthInformation authInformation = new AuthInformation();
        authInformation.setAdmin(currentRoleService.isAdmin(user));
        authInformation.setUser(!authInformation.getAdmin() && currentRoleService.isUser(user));
        authInformation.setGuest(!authInformation.getAdmin() && !authInformation.getUser() && currentRoleService.isGuest(user));

        authInformation.setAdminByNow(currentRoleService.isAdminByNow(user));
        authInformation.setUserByNow(currentRoleService.isUserByNow(user));
        authInformation.setGuestByNow(currentRoleService.isGuestByNow(user));

        return authInformation;
    }

}
