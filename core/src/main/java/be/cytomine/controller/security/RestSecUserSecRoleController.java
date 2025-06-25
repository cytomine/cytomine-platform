package be.cytomine.controller.security;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.security.SecUserSecRoleService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


// TODO IAM: remove?
@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestSecUserSecRoleController extends RestCytomineController {

    private final SecUserSecRoleService secUserSecRoleService;

    private final UserRepository userRepository;

    private  final SecRoleRepository secRoleRepository;

    private final CurrentRoleService currentRoleService;

    @GetMapping("/user/{user}/role.json")
    public ResponseEntity<String> list(
            @PathVariable("user") Long userId,
            @RequestParam(defaultValue = "false") Boolean highest)
    {
        log.debug("REST request to list roles for user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        return responseSuccess(highest ? List.of(secUserSecRoleService.getHighest(user)) : secUserSecRoleService.list(user));
    }

    @GetMapping("/user/{user}/role/{role}.json")
    public ResponseEntity<String> get(
            @PathVariable("user") Long userId,
            @PathVariable("role") Long roleId
    ) {
        log.debug("REST request to list roles for user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        SecRole role = secRoleRepository.findById(roleId)
                .orElseThrow(() -> new ObjectNotFoundException("SecRole", roleId));
        return responseSuccess(secUserSecRoleService.find(user, role)
                .orElseThrow(() -> new ObjectNotFoundException("SecUserSecRole", JsonObject.of("user", user.getId(),"role", role.getId()).toJsonString())));
    }

    @PostMapping("/user/{user}/role.json")
    public ResponseEntity<String> add(
            @PathVariable("user") Long userId,
            @RequestBody JsonObject json
    ) {
        log.debug("REST add role for {}", json.toJsonString());
        return responseSuccess(secUserSecRoleService.add(json));
    }

    @DeleteMapping("/user/{user}/role/{role}.json")
    public ResponseEntity<String> delete(
            @PathVariable("user") Long userId,
            @PathVariable("role") Long roleId
    ) {
        log.debug("REST request to list roles for user {} role {}", userId, roleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        SecRole role = secRoleRepository.findById(roleId)
                .orElseThrow(() -> new ObjectNotFoundException("SecRole", roleId));
        SecUserSecRole secSecUserSecRole = secUserSecRoleService.find(user, role)
                .orElseThrow(() -> new ObjectNotFoundException("SecUserSecRole", JsonObject.of("user", user.getId(), "role", role.getId()).toJsonString()));
        return delete(secUserSecRoleService, secSecUserSecRole.toJsonObject(), null);
    }



    @PutMapping("/user/{user}/role/{role}/define.json")
    public ResponseEntity<String> define(
            @PathVariable("user") Long userId,
            @PathVariable("role") Long roleId
    ) {
        log.debug("REST define role {} for user {}", roleId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        SecRole role = secRoleRepository.findById(roleId)
                .orElseThrow(() -> new ObjectNotFoundException("SecRole", roleId));
        secUserSecRoleService.define(user, role);
        return responseSuccess(new ArrayList(currentRoleService.findCurrentRole(user)));
    }
}
