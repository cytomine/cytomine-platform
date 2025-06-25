package be.cytomine.controller;

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.domain.social.PersistentConnection;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import be.cytomine.repositorynosql.social.PersistentConnectionRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;
import java.util.stream.Stream;

@RestController
@RequestMapping("")
@Slf4j
@RequiredArgsConstructor
public class ServerController extends RestCytomineController {

    private final ApplicationProperties applicationProperties;

    private final CurrentUserService currentUserService;

    private final SequenceService sequenceService;

    private final PersistentConnectionRepository persistentConnectionRepository;

    private final LastConnectionRepository lastConnectionRepository;

    @RequestMapping(value = {"/server/ping.json", "/server/ping"}, method = {RequestMethod.GET, RequestMethod.POST}) // without.json is deprecated
    public ResponseEntity<String> ping(HttpSession session) throws IOException {
        log.debug("REST request to ping");
        JsonObject json = super.mergeQueryParamsAndBodyParams();
        JsonObject response = new JsonObject();
        response.put("alive", true);
        response.put("version", applicationProperties.getVersion());
        response.put("serverURL", applicationProperties.getServerURL());
        response.put("serverID", applicationProperties.getServerId());

        // TODO 2024.2 - LAST CONNECTION (IN A PROJECT)
//        if (isAuthenticated()) {
//            User user = currentUserService.getCurrentUser();
//            response.put("user", user.getId());
////            response.put("shortTermToken", tokenProvider.createToken(SecurityContextHolder.getContext().getAuthentication(), TokenType.SHORT_TERM));
//
////            if (!user.getEnabled()) {
////                log.info("Disabled user. Invalidation of its sessions");
////                session.invalidate();
////            }
//            Long idProject = null;
//            if(!json.getJSONAttrStr("project", "null").equals("null")) {
//                idProject = json.getJSONAttrLong("project");
//            }
//            addLastConnection(user, idProject);
//        }
        return JsonResponseEntity.status(HttpStatus.OK).body(response.toJsonString());
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                getAuthorities(authentication).noneMatch("ROLE_ANONYMOUS"::equals);
    }

    private static Stream<String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority);
    }

    void addLastConnection(User user, Long idProject) {
        try {
            LastConnection connection = new LastConnection();
            connection.setId(sequenceService.generateID());
            connection.setUser(user.getId());
            connection.setDate(new Date());
            connection.setCreated(connection.getDate());
            connection.setProject(idProject);
            lastConnectionRepository.insert(connection); //don't use save (stateless collection)

            PersistentConnection connectionPersist = new PersistentConnection();
            connectionPersist.setId(sequenceService.generateID());
            connectionPersist.setUser(user.getId());
            connectionPersist.setProject(idProject);
            connectionPersist.setCreated(new Date());
            connectionPersist.setSession(RequestContextHolder.currentRequestAttributes().getSessionId());
            persistentConnectionRepository.insert(connectionPersist); //don't use save (stateless collection)
        } catch (NonTransientDataAccessException e) {
            log.error(e.getMessage());
        }
    }

    @GetMapping("/status.json")
    public ResponseEntity<String> status() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("alive", true);
        jsonObject.put("version", applicationProperties.getVersion());
        jsonObject.put("serverURL", applicationProperties.getServerURL());
        return ResponseEntity.ok(jsonObject.toJsonString());
    }
}
