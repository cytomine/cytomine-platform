package be.cytomine.controller.social;

import java.util.Date;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.LastUserPosition;
import be.cytomine.dto.image.AreaDTO;
import be.cytomine.dto.image.Point;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.security.UserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.social.UserPositionService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * Controller for user position
 * Position of the user (x,y) on an image for a time
 */
@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestUserPositionController extends RestCytomineController {

    private final UserPositionService userPositionService;

    private final ImageInstanceService imageInstanceService;

    private final SliceInstanceService sliceInstanceService;

    private final CurrentUserService currentUserService;

    private final UserService userService;

    private final SecurityACLService securityACLService;

    @PostMapping("/imageinstance/{id}/position.json")
    public ResponseEntity<String> addFromImageInstance(
            @PathVariable Long id,
            @RequestBody JsonObject json
    ) {
        log.debug("REST request add user position for imageinstance {id}");
        ImageInstance imageInstance =
                imageInstanceService.find(id).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        SliceInstance referenceSlice = imageInstanceService.getReferenceSlice(imageInstance.getId());
        return add(imageInstance, referenceSlice, json);
    }

    @PostMapping("/sliceinstance/{id}/position.json")
    public ResponseEntity<String> addFromSliceInstance(
            @PathVariable Long id,
            @RequestBody JsonObject json
    ) {
        log.debug("REST request add user position for sliceinstance {}", id);
        SliceInstance referenceSlice = sliceInstanceService.find(id)
                .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", id));
        return add(referenceSlice.getImage(), referenceSlice, json);
    }

    public ResponseEntity<String> add(
            ImageInstance imageInstance,
            SliceInstance sliceInstance,
            JsonObject json
    ) {
        Date date = new Date();
        User user = currentUserService.getCurrentUser();

        Point topLeft = new Point(json.getJSONAttrDouble("topLeftX", 0d), json.getJSONAttrDouble("topLeftY", 0d));
        Point topRight = new Point(json.getJSONAttrDouble("topRightX", 0d), json.getJSONAttrDouble("topRightY", 0d));
        Point bottomRight = new Point(json.getJSONAttrDouble("bottomRightX", 0d), json.getJSONAttrDouble("bottomRightY", 0d));
        Point bottomLeft = new Point(json.getJSONAttrDouble("bottomLeftX", 0d), json.getJSONAttrDouble("bottomLeftY", 0d));
        AreaDTO areaDTO = new AreaDTO(topLeft, topRight, bottomRight, bottomLeft);

        Integer zoom = json.getJSONAttrInteger("zoom", 0);
        Double rotation = json.getJSONAttrDouble("rotation", 0d);
        Boolean broadcast = json.getJSONAttrBoolean("broadcast" , false);

        return responseSuccess(userPositionService.add(date, user, sliceInstance, imageInstance, areaDTO, zoom, rotation, broadcast));
    }

    @GetMapping("/imageinstance/{image}/position/{user}.json")
    public ResponseEntity<String> getLastUserPosition(
            @PathVariable("image") Long imageId,
            @PathVariable("user") Long userId,
            @RequestParam(required = false, defaultValue = "false") Boolean broadcast,
            @RequestParam(required = false) Long sliceId
    ) {
        ImageInstance imageInstance =
                imageInstanceService.find(imageId).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", imageId));
        User user = userService.findUser(userId).orElseThrow(() -> new ObjectNotFoundException("User", userId));
        SliceInstance sliceInstance = null;
        if (sliceId!=null) {
            sliceInstance = sliceInstanceService.find(sliceId)
                    .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", sliceId));
        }
        userPositionService.addAsFollower(user, (User) currentUserService.getCurrentUser(), imageInstance);
        return responseSuccess(userPositionService.lastPositionByUser(imageInstance, sliceInstance, user, broadcast).map(LastUserPosition::toJsonObject).orElse(new JsonObject()));
    }

    @GetMapping(value = {"/imageinstance/{image}/positions.json", "/imageinstance/{image}/position.json"})
    public ResponseEntity<String> list(
            @PathVariable("image") Long imageId,
            @RequestParam(value = "user", required = false) Long userId,
            @RequestParam(value = "slice", required = false) Long sliceId,
            @RequestParam(required = false) Long afterThan,
            @RequestParam(required = false) Long beforeThan,
            @RequestParam(required = false, defaultValue = "false") Boolean showDetails,
            @RequestParam(required = false, defaultValue = "0") Long max,
            @RequestParam(required = false, defaultValue = "0") Long offset
    ) {
        ImageInstance imageInstance =
                imageInstanceService.find(imageId).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", imageId));
        User user = null;
        if (userId!=null) {
            user = userService.find(userId).orElseThrow(() -> new ObjectNotFoundException("User", userId));
        }
        SliceInstance sliceInstance = null;
        if (sliceId!=null) {
            sliceInstance = sliceInstanceService.find(sliceId)
                    .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", sliceId));
        }
        if (showDetails) {
            return responseSuccess(userPositionService.list(imageInstance, user, sliceInstance, afterThan, beforeThan, max.intValue(), offset.intValue()));
        } else {
            return responseSuccess(userPositionService.summarize(imageInstance, user, sliceInstance, afterThan, beforeThan));
        }
    }

    @GetMapping("/imageinstance/{image}/online.json")
    public ResponseEntity<String> listOnlineUsersByImage(
            @PathVariable("image") Long imageId,
            @RequestParam(value = "slice", required = false) Long sliceId,
            @RequestParam(required = false, defaultValue = "false") Boolean broadcast
    ) {
        ImageInstance imageInstance =
                imageInstanceService.find(imageId).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", imageId));
        SliceInstance sliceInstance = null;
        if (sliceId!=null) {
            sliceInstance = sliceInstanceService.find(sliceId)
                    .orElseThrow(() -> new ObjectNotFoundException("SliceInstance", sliceId));
        }
        return responseSuccess(JsonObject.of("users", userPositionService.listOnlineUsersByImage(imageInstance, sliceInstance, broadcast)));
    }

    @GetMapping("/imageinstance/{image}/followers/{user}.json")
    public ResponseEntity<String> listFollowers(
            @PathVariable("image") Long imageId,
            @PathVariable("user") Long userId) {
        log.debug("REST request get list of followers");
        User user = userService.find(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User", userId));
        securityACLService.checkIsSameUser(currentUserService.getCurrentUser(), user);

        ImageInstance imageInstance =
                imageInstanceService.find(imageId).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", imageId));
        return responseSuccess(userPositionService.listFollowers(userId, imageInstance.getId()));
    }
}
