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

import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.ServerException;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.security.current.CurrentUser;
import be.cytomine.security.current.FullCurrentUser;
import be.cytomine.security.current.PartialCurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

// TODO IAM: adapt to get the Cytomine user from IAM reference
@Slf4j
@Service
public class CurrentUserService {

    @Autowired
    private UserRepository userRepository;

    public String getCurrentUsername() {
        CurrentUser currentUser = getSecurityCurrentUser().orElseThrow(() -> new ServerException("Cannot read current user"));
        if (currentUser.isFullObjectProvided() || currentUser.isUsernameProvided()) {
            return currentUser.getUser().getUsername();
        } else {
            throw new ObjectNotFoundException("User", "Cannot read current username. Object " + currentUser + " is not supported");
        }
    }

    public User getCurrentUser() {
        CurrentUser currentUser = getSecurityCurrentUser().orElseThrow(() -> new ServerException("Cannot read current user"));
        User user;
        if (currentUser.isFullObjectProvided()) {
            user = currentUser.getUser();
        } else if(currentUser.isUsernameProvided()) {
            user = userRepository.findByUsernameLikeIgnoreCase(currentUser.getUser().getUsername()).orElseThrow(() -> new ServerException("Cannot find current user with username " + currentUser.getUser().getUsername()));
        } else {
            throw new ObjectNotFoundException("User", "Cannot read current user. Object " + currentUser + " is not supported");
        }
        return user;
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsernameLikeIgnoreCase(username).orElseThrow(() -> new ServerException("Cannot find current user with username " + username));
    }

    public static Optional<CurrentUser> getSecurityCurrentUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractCurrentUser(securityContext.getAuthentication()));
    }

    private static CurrentUser extractCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getDetails() instanceof User) {
            FullCurrentUser fullCurrentUser = new FullCurrentUser();
            fullCurrentUser.setUser((User)authentication.getDetails());
            return fullCurrentUser;
        } else if (authentication.getPrincipal() instanceof String) {
            PartialCurrentUser partialCurrentUser = new PartialCurrentUser();
            partialCurrentUser.setUsername((String)authentication.getPrincipal());
            return partialCurrentUser;
        } else if (authentication.getPrincipal() instanceof UserDetails) {
            PartialCurrentUser partialCurrentUser = new PartialCurrentUser();
            partialCurrentUser.setUsername(((UserDetails) authentication.getPrincipal()).getUsername());
            return partialCurrentUser;
        }else if (authentication instanceof JwtAuthenticationToken) {
            PartialCurrentUser partialCurrentUser = new PartialCurrentUser();
            // this is the preferred_username coming from token claims
            partialCurrentUser.setUsername(authentication.getName());
            return partialCurrentUser;
        }
        return null;
    }

}
