package be.cytomine.controller.security;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.security.SecRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//TODO IAM: remove ?
@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestSecRoleController extends RestCytomineController {

    private final SecRoleService secRoleService;

    @GetMapping("/role.json")
    public ResponseEntity<String> list() {
        log.debug("REST request to list roles");
        return responseSuccess(secRoleService.list());
    }

    @GetMapping("/role/{id}.json")
    public ResponseEntity<String> get(@PathVariable Long id) {
        log.debug("REST request to list roles");
        return responseSuccess(secRoleService.find(id).orElseThrow(() -> new ObjectNotFoundException("SecRole", id)));
    }
}
