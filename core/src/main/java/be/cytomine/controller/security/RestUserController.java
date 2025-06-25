package be.cytomine.controller.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import be.cytomine.config.security.ApiKeyFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import be.cytomine.controller.JsonResponseEntity;
import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.project.ProjectRepresentativeUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.report.ReportService;
import be.cytomine.service.search.UserSearchExtension;
import be.cytomine.service.security.UserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;


import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestUserController extends RestCytomineController {

    private final UserService userService;

    private final ProjectService projectService;

    private final CurrentUserService currentUserService;

    private final ImageInstanceService imageInstanceService;

    private final SecurityACLService securityACLService;

    private final ProjectRepresentativeUserService projectRepresentativeUserService;

    private final OntologyService ontologyService;

    private final StorageService storageService;

    private final ReportService reportService;

    @GetMapping("/project/{id}/admin.json")
    public ResponseEntity<String> showAdminByProject(
            @PathVariable Long id
    ) {
        log.debug("REST request to list admins from project {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
        return responseSuccess(userService.listAdmins(project), isFilterRequired());
    }

    @GetMapping("/project/{id}/users/representative.json")
    public ResponseEntity<String> showRepresentativeByProject(
            @PathVariable Long id
    ) {
        log.debug("REST request to list representatives from project {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
        return responseSuccess(projectRepresentativeUserService.listByProject(project).stream().map(ProjectRepresentativeUser::getUser)
                .collect(Collectors.toList()), isFilterRequired());
    }

    @GetMapping("/project/{id}/creator.json")
    public ResponseEntity<String> showCreatorByProject(
            @PathVariable Long id
    ) {
        log.debug("REST request to list creator from project {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
        return responseSuccess(List.of(userService.findCreator(project).orElseThrow(() -> new ObjectNotFoundException("Project", "CREATOR"))));
    }

    @GetMapping("/ontology/{id}/user.json")
    public ResponseEntity<String> showUserByOntology(
            @PathVariable Long id
    ) {
        log.debug("REST request to list user from ontology {}", id);
        Ontology ontology = ontologyService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Ontology", id));
        return responseSuccess(userService.listUsers(ontology), isFilterRequired());
    }

    @GetMapping("/project/{id}/userlayer.json")
    public ResponseEntity<String> showLayerByProject(
            @PathVariable Long id,
            @RequestParam(value = "image", required = false, defaultValue = "0") Long idImage
    ) {
        log.debug("REST request to list user layers from project {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));
        ImageInstance image  = imageInstanceService.find(idImage).orElse(null);

        return responseSuccess(userService.listLayers(project, image), isFilterRequired());

    }

    @GetMapping("/storage/{id}/user.json")
    public ResponseEntity<String> showUserByStorage(
            @PathVariable Long id
    ) {
        log.debug("REST request to list user from storage {}", id);
        Storage storage = storageService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Storage", id));
        return responseSuccess(userService.listUsers(storage), isFilterRequired());
    }

    @GetMapping("/user.json")
    public ResponseEntity<String> list(
            @RequestParam(value = "sort", defaultValue = "username", required = false) String sortColumn,
            @RequestParam(value = "order", defaultValue = "asc", required = false) String sortDirection,
            @RequestParam(value = "offset", defaultValue = "0", required = false) Long offset,
            @RequestParam(value = "max", defaultValue = "0", required = false) Long max
    ) {
        log.debug("REST request to list user");
        return responseSuccess(
                userService.list(retrieveSearchParameters(), sortColumn, sortDirection, max, offset)
                , isFilterRequired()
        );
    }

    @GetMapping("/user/{id}.json")
    public ResponseEntity<String> getUser(
            @PathVariable String id
    ) {
        log.debug("REST request to get User : {}", id);

        Optional<User> user = userService.find(id);
        if (user.isEmpty()) {
            user = userService.findByUsername(id);
        }

        return user.map(u -> responseSuccess(u, isFilterRequired())).orElseGet(() -> responseNotFound("User", id));
    }

    /** Deprecated API keys. Will be removed in a future release **/
    @Deprecated
    @GetMapping("/userkey/{publicKey}/keys.json")
    public ResponseEntity<String> keys(@PathVariable String publicKey) {
        User user = userService.findByPublicKey(publicKey)
                .orElseThrow(() -> new ObjectNotFoundException("User", Map.of("publicKey", publicKey).toString()));
        securityACLService.checkIsSameUser(user, currentUserService.getCurrentUser());
        return responseSuccess(JsonObject.of("privateKey", user.getPrivateKey(), "publicKey", user.getPublicKey()));
    }

    @Deprecated
    @GetMapping("/user/{id}/keys.json")
    public ResponseEntity<String> keysById(@PathVariable String id) {
        User user = userService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("User", Map.of("id or username", id).toString()));
        securityACLService.checkIsSameUser(user, currentUserService.getCurrentUser());
        return responseSuccess(JsonObject.of("privateKey", user.getPrivateKey(), "publicKey", user.getPublicKey()));
    }

    @Deprecated
    @GetMapping("/signature.json")
    public ResponseEntity<String> signature(
            @RequestParam(defaultValue = "GET") String method,
            @RequestParam(value = "content-MD5", required = false, defaultValue = "") String contentMD5,
            @RequestParam(value = "content-type", required = false, defaultValue = "") String contenttype,
            @RequestParam(value = "content-Type", required = false, defaultValue = "") String contentType,
            @RequestParam(value = "date", required = false, defaultValue = "") String date,
            @RequestParam(value = "queryString", required = false, defaultValue = "") String queryString,
            @RequestParam(value = "forwardURI", required = false, defaultValue = "") String forwardURI
    ) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        User user = currentUserService.getCurrentUser();
        if (!queryString.isEmpty()) {
            queryString = "?" + queryString;
        }
        String signature = ApiKeyFilter.generateKeys(method,contentMD5,contenttype.isEmpty()?contentType:contenttype,date,queryString,forwardURI,user);

        return responseSuccess(JsonObject.of("signature", signature, "publicKey", user.getPublicKey()));
    }

    @GetMapping("/user/current/keys")
    public ResponseEntity<String> getCurrentUserKeys() {
        User user = currentUserService.getCurrentUser();
        return responseSuccess(JsonObject.of(
                "primaryKey", user.getPublicKey(),
                "secondaryKey", user.getPrivateKey())
        );
    }

    @PostMapping("/user/current/keys")
    public ResponseEntity<String> regenerateCurrentUserKeys() {
        User user = userService.regenerateKeys(currentUserService.getCurrentUser());
        return responseSuccess(JsonObject.of(
                "primaryKey", user.getPublicKey(),
                "secondaryKey", user.getPrivateKey())
        );
    }
    /******************************************************************************************************************/


    @GetMapping("/user/current.json")
    public ResponseEntity<String> getCurrentUser(
    ) {
        log.debug("REST request to get current User");
        return responseSuccess(currentUserService.getCurrentUser());
    }

    //TODO IAM: refactor so that only ADMIN role can create IAM ACCOUNT
//    @PostMapping("/user.json")
//    public ResponseEntity<String> createUser(@RequestBody String json) {
//        log.debug("REST request to save User : " + json);
//        return add(userService, json);
//    }

    //TODO IAM: refactor so that only ADMIN role can modify IAM ACCOUNT
//    @PutMapping("/user/{id}.json")
//    public ResponseEntity<String> updateUser(@PathVariable String id, @RequestBody JsonObject json) {
//        log.debug("REST request to update User : {}", id);
//        return update(userService, json);
//    }

    //TODO IAM: refactor so that only ADMIN role can delete IAM ACCOUNT
//    @DeleteMapping("/user/{id}.json")
//    public ResponseEntity<String> deleteUser(@PathVariable String id) {
//        log.debug("REST request to delete User: {}", id);
//        return delete(userService, JsonObject.of("id", Long.parseLong(id)), null);
//    }


    @GetMapping("/project/{id}/user.json")
    public ResponseEntity<String> showByProject(
            @PathVariable Long id,
            @RequestParam(value = "withLastImage", defaultValue = "false", required = false) Boolean withLastImage,
            @RequestParam(value = "withLastConsultation", defaultValue = "false", required = false) Boolean withLastConsultation,
            @RequestParam(value = "withNumberConsultations", defaultValue = "false", required = false) Boolean withNumberConsultations,
            @RequestParam(value = "sortColumn", defaultValue = "created", required = false) String sortColumn,
            @RequestParam(value = "sortDirection", defaultValue = "desc", required = false) String sortDirection,
            @RequestParam(value = "offset", defaultValue = "0", required = false) Long offset,
            @RequestParam(value = "max", defaultValue = "0", required = false) Long max
    ) {
        log.debug("REST request to list user from project {}", id);
        Project project = projectService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("Project", id));

        UserSearchExtension userSearchExtension = new UserSearchExtension();
        userSearchExtension.setWithLastImage(withLastImage);
        userSearchExtension.setWithLastConnection(withLastConsultation);
        userSearchExtension.setWithNumberConnections(withNumberConsultations);

        return responseSuccess(
                userService.listUsersExtendedByProject(project, userSearchExtension, retrieveSearchParameters(), sortColumn, sortDirection, max, offset)
                , isFilterRequired()
        );
    }

    @PostMapping("/project/{project}/user/{user}.json")
    public ResponseEntity<String> addUserToProject(@PathVariable("project") Long projectId, @PathVariable("user") Long userId) {
        log.debug("REST request to add User {} to project {}", userId, projectId);
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        userService.addUserToProject(user, project, false);
        return responseSuccess(JsonObject.of("data", JsonObject.of("message", "OK")).toJsonString());
    }

    @PostMapping("/project/{project}/user.json")
    public ResponseEntity<String> addUsersToProject(@PathVariable("project") Long projectId, @RequestParam("users") String userIds) {
        log.debug("REST request to add Users {} to project {}", userIds, projectId);
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        List<String> usersIds = Arrays.stream(userIds.split(",")).toList();

        String errorMessage = "";
        List<String> errors = new ArrayList<>();
        List<String> wrongIds = new ArrayList<>();
        List<Long> usersValidIds = new ArrayList<>();
        for (String userId : usersIds) {
            try{
                usersValidIds.add(Long.parseLong(userId));
            } catch(NumberFormatException e){
                wrongIds.add(userId);
            }
        }

        List<User> users = userService.list(usersValidIds);

        wrongIds.addAll(usersIds);
        wrongIds.removeAll(users.stream().map(x -> String.valueOf(x.getId())).toList());

        for (User user : users) {
            try {
                userService.addUserToProject(user, project, false);
            } catch(Exception e) {
                errors.add(user.getId().toString());
            }
        }
        if(!errors.isEmpty()) {
            errorMessage += "Cannot add theses users to the project ${project.id} : "+ String.join(",", errors)+". ";
        }
        if(!wrongIds.isEmpty()) {
            errorMessage += String.join(",", wrongIds) +" are not well formatted ids";
        }

        JsonObject response = new JsonObject();
        response.put("data", JsonObject.of("message", "OK"));
        response.put("status", 200);

        if (!errors.isEmpty()  || !wrongIds.isEmpty()) {
            response.put("data", JsonObject.of("message", errorMessage));
            return JsonResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response.toJsonString());
        } else {
            response.put("data", JsonObject.of("message", "OK"));
            response.put("status", 200);
            return JsonResponseEntity.status(HttpStatus.OK).body(response.toJsonString());
        }
    }

    @DeleteMapping("/project/{project}/user/{user}.json")
    public ResponseEntity<String> deleteUserFromProject(@PathVariable("project") Long projectId, @PathVariable("user") Long userId) {
        log.debug("REST request to remove User {} from project {}", userId, projectId);
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        userService.deleteUserFromProject(user, project, false);
        return responseSuccess(JsonObject.of("data", JsonObject.of("message", "OK")).toJsonString());
    }

    @DeleteMapping("/project/{project}/user.json")
    public ResponseEntity<String> deleteUsersFromProject(@PathVariable("project") Long projectId, @RequestParam("users") String userIds) {
        log.debug("REST request to add Users {} to project {}", userIds, projectId);
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        List<String> usersIds = Arrays.stream(userIds.split(",")).toList();

        String errorMessage = "";
        List<String> errors = new ArrayList<>();
        List<String> wrongIds = new ArrayList<>();
        List<Long> usersValidIds = new ArrayList<>();
        for (String userId : usersIds) {
            try{
                usersValidIds.add(Long.parseLong(userId));
            } catch(NumberFormatException e){
                wrongIds.add(userId);
            }
        }

        List<User> users = userService.list(usersValidIds);

        wrongIds.addAll(usersIds);
        wrongIds.removeAll(users.stream().map(x -> String.valueOf(x.getId())).toList());

        for (User user : users) {
            try {
                userService.deleteUserFromProject(user, project, false);
            } catch(Exception e) {
                errors.add(user.getId().toString());
            }
        }
        if(!errors.isEmpty()) {
            errorMessage += "Cannot add theses users to the project ${project.id} : "+ String.join(",", errors)+". ";
        }
        if(!wrongIds.isEmpty()) {
            errorMessage += String.join(",", wrongIds) +" are not well formatted ids";
        }

        JsonObject response = new JsonObject();
        response.put("data", JsonObject.of("message", "OK"));
        response.put("status", 200);

        if (!errors.isEmpty()  || !wrongIds.isEmpty()) {
            response.put("data", JsonObject.of("message", errorMessage));
            return JsonResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response.toJsonString());
        } else {
            response.put("data", JsonObject.of("message", "OK"));
            response.put("status", 200);
            return JsonResponseEntity.status(HttpStatus.OK).body(response.toJsonString());
        }
    }

    @PostMapping("/project/{project}/user/{user}/admin.json")
    public ResponseEntity<String> addUserAdminToProject(@PathVariable("project") Long projectId, @PathVariable("user") Long userId) {
        log.debug("REST request to add User {} to project {}", userId, projectId);
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        userService.addUserToProject(user, project, true);
        return responseSuccess(JsonObject.of("data", JsonObject.of("message", "OK")).toJsonString());
    }

    @DeleteMapping("/project/{project}/user/{user}/admin.json")
    public ResponseEntity<String> deleteUserAdminFromProject(@PathVariable("project") Long projectId, @PathVariable("user") Long userId) {
        log.debug("REST request to remove User {} from project {}", userId, projectId);
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        if (!Objects.equals(currentUserService.getCurrentUser().getId(), user.getId())) {
            securityACLService.check(project,ADMINISTRATION);
        }
        userService.deleteUserFromProject(user, project, true);
        return responseSuccess(JsonObject.of("data", JsonObject.of("message", "OK")).toJsonString());
    }

    @PostMapping("/storage/{storage}/user/{user}.json")
    public ResponseEntity<String> addUserToStorage(@PathVariable("storage") Long storageId, @PathVariable("user") Long userId) {
        log.debug("REST request to add User {} to storage {}", userId, storageId);
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        Storage storage = storageService.find(storageId)
                .orElseThrow(() -> new ObjectNotFoundException("Storage", storageId));
        userService.addUserToStorage(user, storage);
        return responseSuccess(JsonObject.of("data", JsonObject.of("message", "OK")).toJsonString());
    }

    @DeleteMapping("/storage/{storage}/user/{user}.json")
    public ResponseEntity<String> deleteUserFromStorage(@PathVariable("storage") Long storageId, @PathVariable("user") Long userId) {
        log.debug("REST request to remove User {} from storage {}", userId, storageId);
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        Storage storage = storageService.find(storageId)
                .orElseThrow(() -> new ObjectNotFoundException("Storage", storageId));
        userService.deleteUserFromStorage(user, storage);
        return responseSuccess(JsonObject.of("data", JsonObject.of("message", "OK")).toJsonString());
    }

    // TODO IAM: what about online
    @GetMapping("/user/{id}/friends.json")
    public ResponseEntity<String> listFriends(
            @PathVariable Long id,
            @RequestParam(value = "project", required = false) Long projectId,
            @RequestParam(value = "offline", required = false, defaultValue = "false") Boolean offlineToo
    ) {
        log.debug("REST request to list user layers from project {}", id);

        User user = userService.findUser(id)
                .orElseThrow(() -> new ObjectNotFoundException("User", id));

        List<User> friends = new ArrayList<>();

        if (offlineToo) {
            if (projectId!=null) {
                //get all user project list
                Project project = projectService.find(projectId)
                        .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
                friends = userService.listUsers(project);
            } else {
                //get all people that share common project with user
                friends = userService.getAllFriendsUsers(user);
            }
        } else {
            if (projectId!=null) {
                //get user project online
                Project project = projectService.find(projectId)
                        .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
                friends = userService.getAllFriendsUsersOnline(user, project);
            } else {
                //get friends online
                friends = userService.getAllFriendsUsersOnline(user);
            }
        }
        return responseSuccess(friends, isFilterRequired());
    }

    @GetMapping("/project/{project}/online/user.json")
    public ResponseEntity<String> listOnlineFriendsWithPosition(
            @PathVariable(value = "project") Long projectId
    ) {
        log.debug("REST request to list online user for project {}", projectId);

        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(userService.getAllOnlineUserWithTheirPositions(project), isFilterRequired());
    }

    @GetMapping("/project/{project}/usersActivity.json")
    public ResponseEntity<String> usersActivity(
            @PathVariable(value = "project") Long projectId
    ) {
        log.debug("REST request to list online user for project {}", projectId);

        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(userService.getUsersWithLastActivities(project), isFilterRequired());
    }

    @GetMapping("/project/{project}/user/download")
    public void download(
            @PathVariable(value = "project") Long projectId,
            @RequestParam String format
    ) throws IOException {
        log.debug("REST request to download user listing from a specific project : {}", projectId);

        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        List<User> projectUsers = userService.listUsers(project);
        List<Map<String, Object>> users = new ArrayList<>();

        for (User user : projectUsers) {
            if (user instanceof User) {
                users.add(Map.of(
                        "username", ((User) user).getUsername(),
                        "name", (((User) user).getName())
                ));
            }

            byte[] report = reportService.generateUsersReport(project.getName(), users, format);
            responseReportFile(reportService.getUsersReportFileName(format, projectId), report, format);
        }
    }

    @GetMapping("/project/{project}/resumeActivity/{user}.json")
    public ResponseEntity<String> resumeActivity(
            @PathVariable(value = "project") Long projectId,
            @PathVariable(value = "user") Long userId
    ) {
        log.debug("REST request to list activities for user {} and for project {}", userId, projectId);

        User user = userService.findUser(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));

        return responseSuccess(userService.getResumeActivities(project, user), isFilterRequired());
    }

    boolean isFilterRequired() {
        try{
            securityACLService.checkAdmin(currentUserService.getCurrentUser());
            return false;
        } catch(ForbiddenException e){}
        return true;
    }
}
