package be.cytomine.service.social;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.LastUserPosition;
import be.cytomine.domain.social.PersistentUserPosition;
import be.cytomine.dto.image.AreaDTO;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.repositorynosql.social.*;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.security.UserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Projections;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static java.util.stream.Collectors.*;
import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
public class UserPositionService {

    static final int USER_UNFOLLOWING_DELAY = 10;
    public static final String DATABASE_NAME = "cytomine";
    @Autowired
    CurrentUserService currentUserService;

    @Autowired
    UserService userService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    ProjectConnectionRepository projectConnectionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MongoClient mongoClient;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    AnnotationListingService annotationListingService;

    @Autowired
    LastConnectionRepository lastConnectionRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    WebSocketUserPositionHandler webSocketUserPositionHandler;

    @Autowired
    PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    SequenceService sequenceService;

//
//    public LastUserPosition add(User user, SliceInstance sliceInstance) {
//
//    }
//
//    public LastUserPosition add(User user, ImageInstance imageInstance) {
//
//    }

    // usersTracked key -> "trackedUserId/imageId"
    public static Map<String, List<User>> broadcasters = new ConcurrentHashMap<>();

    // usersTracking key -> "followerId/imageId"
    public static Map<String, Boolean> followers = new ConcurrentHashMap<>();

    public PersistentUserPosition add(
            Date created,
            User user,
            SliceInstance sliceInstance,
            ImageInstance imageInstance,
            AreaDTO area,
            Integer zoom,
            Double rotation,
            Boolean broadcast) {

        Optional<LastUserPosition> lastPosition = lastPositionByUser(imageInstance, sliceInstance, user, broadcast);

        //TODO: no ACL???
        LastUserPosition position = new LastUserPosition();
        position.setId(sequenceService.generateID());
        position.setUser(user.getId());
        position.setImage(imageInstance.getId());
        position.setSlice(sliceInstance.getId());
        position.setProject(imageInstance.getProject().getId());
        List<List<Double>> currentLocation = area.toMongodbLocation().getCoordinates();
        position.setLocation(currentLocation);
        position.setZoom(zoom);
        position.setRotation(rotation);
        position.setBroadcast(broadcast);
        position.setCreated(created);
        position.setUpdated(created);
        position.setImageName(imageInstance.getBlindInstanceFilename());
        lastUserPositionRepository.insert(position);

        PersistentUserPosition persistedPosition = new PersistentUserPosition();
        persistedPosition.setId(sequenceService.generateID());
        persistedPosition.setUser(user.getId());
        persistedPosition.setImage(imageInstance.getId());
        persistedPosition.setSlice(sliceInstance.getId());
        persistedPosition.setProject(imageInstance.getProject().getId());
        persistedPosition.setLocation(area.toMongodbLocation().getCoordinates());
        persistedPosition.setZoom(zoom);
        persistedPosition.setRotation(rotation);
        persistedPosition.setBroadcast(broadcast);
        persistedPosition.setCreated(created);
        persistedPosition.setUpdated(created);
        persistedPosition.setImageName(imageInstance.getBlindInstanceFilename());
        persistentUserPositionRepository.insert(persistedPosition);

        return persistedPosition;
    }

    public void addAsFollower(User broadcaster, User follower, ImageInstance imageInstance){
        String broadcasterAndImageId = broadcaster.getId().toString() + "/" + imageInstance.getId().toString();
        String followerAndImageId = follower.getId().toString() + "/" + imageInstance.getId().toString();

        if (broadcasters.containsKey(broadcasterAndImageId)) {
            Set<Long> userIds = broadcasters.get(broadcasterAndImageId).stream()
                    .map(CytomineDomain::getId).collect(toSet());

            if(!userIds.contains(follower.getId())){
                broadcasters.get(broadcasterAndImageId).add(follower);
                followers.put(followerAndImageId, true);
            }else{
                // Mark as fetching
                followers.replace(followerAndImageId, followers.get(followerAndImageId), true);
            }
        } else {
            broadcasters.put(broadcasterAndImageId, new ArrayList<>(Collections.singleton(follower)));
            followers.put(followerAndImageId, true);
        }
    }

    public Optional<LastUserPosition> lastPositionByUser(ImageInstance image, SliceInstance slice, User user, boolean broadcast) {
        securityACLService.check(image,READ);

        return getLastUserPosition(image, slice, user, broadcast);
    }

    /**
     * TODO Do not bypass ACL checks.
     * Temporary solution to the WebSocket issue
     */
    public Optional<LastUserPosition> lastPositionByUserBypassACL(ImageInstance image, SliceInstance slice, User user, boolean broadcast) {
        return getLastUserPosition(image, slice, user, broadcast);
    }

    private Optional<LastUserPosition> getLastUserPosition(ImageInstance image, SliceInstance slice, User user, boolean broadcast) {
        Query query = new Query();
        query.addCriteria(Criteria.where("user").is(user.getId()));
        query.addCriteria(Criteria.where("image").is(image.getId()));
        if (slice!=null) {
            query.addCriteria(Criteria.where("slice").is(slice.getId()));
        }
        if (broadcast) {
            query.addCriteria(Criteria.where("broadcast").is(true));
        }
        query.with(Sort.by(Sort.Direction.DESC, "created"));
        query.limit(1);

        List<LastUserPosition> lastUserPositions = mongoTemplate.find(query, LastUserPosition.class);
        return lastUserPositions.stream().findFirst();
    }

    public List<Long> listOnlineUsersByImage(ImageInstance image, SliceInstance slice, boolean broadcast) {
        securityACLService.check(image,READ);
        Date thirtySecondsAgo = DateUtils.addSeconds(new Date(), -30);

        List<Bson> request = new ArrayList<>();
        request.add(match(eq("image", image.getId())));
        request.add(match(gte("created", thirtySecondsAgo)));
        if (broadcast) {
            request.add(match(eq("broadcast", true)));
        }
        if (slice!=null) {
            request.add(match(eq("slice", slice.getId())));
        }

        request.add(project(Projections.fields(Projections.computed("user", "$user"))));
        request.add(group("$user"));


        MongoCollection<Document> persistentProjectConnection = mongoClient.getDatabase(DATABASE_NAME).getCollection("lastUserPosition");

        List<Document> results = persistentProjectConnection.aggregate(request)
                .into(new ArrayList<>());

        return results.stream().map(x -> x.getLong("_id")).toList();
    }

    public List<PersistentUserPosition> list(ImageInstance image, User user, SliceInstance slice, Long afterThan, Long beforeThan, Integer max, Integer offset){
        securityACLService.check(image,WRITE);
        if (max == 0) {
            max = Integer.MAX_VALUE;
        }

        Query query = new Query();
        if (user!=null) {
            query.addCriteria(Criteria.where("user").is(user.getId()));
        }
        if (image!=null) {
            query.addCriteria(Criteria.where("image").is(image.getId()));
        }
        if (slice!=null) {
            query.addCriteria(Criteria.where("slice").is(slice.getId()));
        }
        if (afterThan!=null && beforeThan!=null) {
            query.addCriteria(Criteria.where("created").gte(new Date(afterThan)).lte(new Date(beforeThan)));
        } else if (afterThan!=null) {
            query.addCriteria(Criteria.where("created").gte(new Date(afterThan)));
        } else if (beforeThan!=null) {
            query.addCriteria(Criteria.where("created").lte(new Date(beforeThan)));
        }
        query.with(Sort.by(Sort.Direction.DESC, "created"));
        query.limit(max);
        query.skip(offset);

        List<PersistentUserPosition> lastUserPositions = mongoTemplate.find(query, PersistentUserPosition.class);
        return lastUserPositions;

    }

    public List<String> listFollowers(Long userId, Long imageId){
        String userAndImageId = userId.toString()+"/"+imageId.toString();
        List<String> followersIds = new ArrayList<>();

        ConcurrentWebSocketSessionDecorator broadcastSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        if(broadcastSession!=null){
            ConcurrentWebSocketSessionDecorator[] followers = WebSocketUserPositionHandler.sessionsTracked.get(broadcastSession);
            if(followers != null) {
                followersIds = webSocketUserPositionHandler.getSessionsUserIds(followers).stream().distinct().toList();
            }
        }

        List<User> poolingUsers = broadcasters.get(userAndImageId);
        if(poolingUsers != null){
            for(User user : poolingUsers){
                if(!followersIds.contains(user.getId().toString())){
                    List<String> followersIdsList = new ArrayList<>(followersIds);
                    followersIdsList.add(user.getId().toString());
                    followersIds = followersIdsList;
                }
            }
        }
        return followersIds;
    }

    public List<Map<String, Object>> summarize(ImageInstance image, User user, SliceInstance slice, Long afterThan, Long beforeThan) {
        securityACLService.check(image, WRITE);

        List<Bson> request = new ArrayList<>();
        request.add(match(eq("image", image.getId())));
        if (afterThan != null) {
            request.add(match(gte("created", new Date(afterThan))));
        }
        if (beforeThan != null) {
            request.add(match(lte("created", new Date(beforeThan))));
        }
        if (user != null) {
            request.add(match(eq("user", user.getId())));
        }
        if (slice != null) {
            request.add(match(eq("slice", slice.getId())));
        }


        request.add(group(Document.parse("{location: '$location', zoom: '$zoom', rotation: '$rotation'}"),
                Accumulators.sum("frequency", 1), Accumulators.first("image", "$image")));


        MongoCollection<Document> persistentUserPosition = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentUserPosition");

        List<Document> results = persistentUserPosition.aggregate(request)
                .into(new ArrayList<>());

        return results.stream().map(x ->
                JsonObject.of("location", ((Document) x.get("_id")).get("location"),
                        "zoom", ((Document) x.get("_id")).get("zoom"),
                        "rotation", ((Document) x.get("_id")).get("rotation"),
                        "rotation", ((Document) x.get("_id")).get("rotation"),
                        "frequency", x.get("frequency"),
                        "image", x.get("image"))).collect(toList());

    }


    public List<JsonObject> findUsersPositions(Project project) {

        //Get all user online and their pictures
        Date thirtySecondsAgo = DateUtils.addSeconds(new Date(), -30);

        List<Bson> request = new ArrayList<>();
        request.add(match(eq("project", project.getId())));
        request.add(match(gt("created", thirtySecondsAgo)));

        request.add(project(Document.parse("{user: 1, image: 1, slice: 1, imageName: 1, created: 1}")));


        request.add(group(Document.parse("{user: '$user', slice: '$slice'}"),
                Accumulators.max("date", "$created"), Accumulators.first("image", "$image"), Accumulators.first("imageName", "$imageName")));

        request.add(group(Document.parse("{user: '$_id.user'}"),
                Accumulators.push("position", Document.parse("{id: '$_id.image',slice: '$_id.slice', image: '$image', filename: '$imageName', originalFilename: '$imageName', date: '$date'}"))));

        MongoCollection<Document> persistentUserPosition = mongoClient.getDatabase(DATABASE_NAME).getCollection("lastUserPosition");

        List<Document> results = persistentUserPosition.aggregate(request)
                .into(new ArrayList<>());

        List<JsonObject> usersWithPosition =  results.stream().map(x ->
                JsonObject.of("id", ((Document) x.get("_id")).get("user"),
                        "position", x.get("position"))).collect(toList());

        return usersWithPosition;
    }

    public void removeFollower(Map.Entry<String, Boolean> entry){
        String[] splitEntry = entry.getKey().split("/");
        String followerId = splitEntry[0];
        String imageId = splitEntry[1];

        // For each broadcaster
        for(Map.Entry<String, List<User>> trackedEntry : broadcasters.entrySet()){
            String broadcaster = trackedEntry.getKey();
            String entryImageId = broadcaster.split("/")[1];

            // If he is same image
            if(entryImageId.equals(imageId)){
                Set<String> userIds = trackedEntry.getValue().stream()
                        .map(user -> user.getId().toString()).collect(toSet());
                // and one of his follower id is followerId
                if(userIds.contains(followerId)){
                    removeFollower(Long.parseLong(followerId), broadcaster);
                }
            }
        }
    }

    private void removeFollower(Long followerId, String broadcaster){
        List<User> newFollowers = new ArrayList<>();
        for(User user : broadcasters.get(broadcaster)){
            if(!user.getId().equals(followerId)){
                newFollowers.add(user);
            }
        }
        broadcasters.replace(broadcaster, newFollowers);
    }

    @PostConstruct
    public void positionScheduler(){
        DelegatingSecurityContextScheduledExecutorService delegatedScheduler = new DelegatingSecurityContextScheduledExecutorService(
                Executors.newScheduledThreadPool(1),
                SecurityContextHolder.getContext()
        );
        delegatedScheduler.scheduleAtFixedRate(
            () -> {
                for(Map.Entry<String, Boolean> entry : followers.entrySet()){
                    if(!entry.getValue()){
                        followers.remove(entry.getKey());
                        removeFollower(entry);
                    }else{
                        followers.replace(entry.getKey(), true, false);
                    }
                }
            }, USER_UNFOLLOWING_DELAY, USER_UNFOLLOWING_DELAY, TimeUnit.SECONDS);
    }
}
