package be.cytomine.controller.security;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.CytomineException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.security.AclAuthService;
import be.cytomine.utils.JsonObject;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class RestACLController extends RestCytomineController {

    private final EntityManager entityManager;

    private final AclAuthService aclAuthService;

    @GetMapping("/domain/{domainClassName}/{domainIdent}/user/{user}")
    public ResponseEntity<String> getPermission(
            @PathVariable String domainClassName,
            @PathVariable String domainIdent,
            @PathVariable String user
    ) {
        log.debug("REST request to get permission : {} {} {}", domainClassName, domainIdent, user);

        try {
            if(domainClassName!=null && domainIdent!=null && user!=null) {
                User secUser = entityManager.find(User.class, Long.parseLong(user));
                return responseSuccess(aclAuthService.get(Long.parseLong(domainIdent),secUser));
            } else {
                throw new ObjectNotFoundException("Request not valid: domainClassName="+ domainClassName + ", domainIdent= " + domainIdent + ", user=" + user);
            }
        } catch(CytomineException e) {
            return ResponseEntity.status(e.code).contentType(MediaType.APPLICATION_JSON).body(JsonObject.of("success", false, "errors", e.msg).toJsonString());
        }
    }
}
