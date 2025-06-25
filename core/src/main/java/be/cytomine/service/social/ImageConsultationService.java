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

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.PersistentImageConsultation;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.domain.social.PersistentUserPosition;
import be.cytomine.exceptions.CytomineException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.AnnotationListing;
import be.cytomine.repository.UserAnnotationListing;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.repositorynosql.social.*;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class ImageConsultationService {

    public static final String DATABASE_NAME = "cytomine";
    @Autowired
    CurrentUserService currentUserService;

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
    private SessionFactory sessionFactory;

    @Autowired
    ImageInstanceRepository imageInstanceRepository;

    @Autowired
    PersistentImageConsultationRepository persistentImageConsultationRepository;

    @Autowired
    PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    ImageInstanceService imageInstanceService;

    public PersistentImageConsultation add(User user, Long imageId, String session, String mode, Date created) {
        System.out.println(currentUserService.getCurrentUser());
        ImageInstance imageInstance = imageInstanceRepository.findById(imageId)
                .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", imageId));

        closeLastImageConsultation(user.getId(), imageId, created);

        PersistentImageConsultation consultation = new PersistentImageConsultation();
        consultation.setId(sequenceService.generateID());
        consultation.setUser(user.getId());
        consultation.setImage(imageId);
        consultation.setProject(imageInstance.getProject().getId());
        consultation.setSession(session);

        Optional<PersistentProjectConnection> persistentImageConsultation = persistentProjectConnectionRepository.findAllByUserAndProject(
                user.getId(),
                imageInstance.getProject().getId(),
                PageRequest.of(0, 1, Sort.Direction.DESC, "created")).stream().findFirst();


        consultation.setProjectConnection(persistentImageConsultation.map(x -> x.getId()).orElse(null));

        consultation.setCreated(created);
        consultation.setSession(session);
        consultation.setMode(mode);
        consultation.setImageName(imageInstance.getBlindInstanceFilename());
        consultation.setImageThumb(UrlApi.getImageInstanceThumbUrlWithMaxSize(imageInstance.getId(), 512, "png"));


        persistentImageConsultationRepository.insert(consultation);

        return consultation;
    }


    private void closeLastImageConsultation(Long user, Long image, Date before) {
        Optional<PersistentImageConsultation> consultation = persistentImageConsultationRepository.findAllByUserAndImageAndCreatedLessThan(user, image, before, PageRequest.of(0, 1, Sort.Direction.DESC, "created"))
                .stream().findFirst();

        //first consultation
        if (consultation.isEmpty()) {
            return;
        }

        //last consultation already closed
        if (consultation.get().getTime() != null) {
            return;
        }

        fillImageConsultation(consultation.get(), before);

        persistentImageConsultationRepository.save(consultation.get());
    }


    public void fillImageConsultation(PersistentImageConsultation consultation, Date before) {
        Date after = consultation.getCreated();

        AggregationResults positions = persistentUserPositionRepository
                .retrieve(consultation.getProject(), consultation.getUser(), consultation.getImage(), before, after, new Date(0));


        if (!positions.getMappedResults().isEmpty()) {
            log.debug(positions.toString());
        }

        List<Long> continuousConnections = (List<Long>)positions.getMappedResults().stream().map(x ->
                x instanceof LinkedHashMap ? be.cytomine.utils.DateUtils.computeDateInMillis((Date)((LinkedHashMap) x).get("created")) :
                        be.cytomine.utils.DateUtils.computeDateInMillis((Date)((PersistentUserPosition) x).getCreated())).collect(Collectors.toList());


        //we calculated the gaps between connections to identify the period of non activity
        List<Long> continuousConnectionIntervals = new ArrayList<>();

        Long first = consultation.getCreated().getTime();
        for (Long time : continuousConnections) {
            continuousConnectionIntervals.add(time - first);
            first = time;
        }

        consultation.setTime(continuousConnectionIntervals.stream().filter(x -> x < 15000L).reduce(0L, Long::sum));

        if (consultation.getTime() == null) {
            consultation.setTime(0L);
        }

        AnnotationListing al = new UserAnnotationListing(entityManager);
        al.setProject(consultation.getProject());
        al.setUser(consultation.getUser());
        al.setImage(consultation.getImage());
        al.setBeforeThan(before);
        al.setAfterThan(after);

        // count created annotations

        consultation.setCountCreatedAnnotations(annotationListingService.listGeneric(al).size());
        persistentImageConsultationRepository.save(consultation);

    }

    public Page<PersistentImageConsultation> listImageConsultationByProjectAndUserNoImageDistinct(Project project, User user, Integer max, Integer offset) {
        securityACLService.checkIsSameUserOrAdminContainer(project, user, currentUserService.getCurrentUser());
        if (max != 0) {
            max += offset; // ?
        } else {
            max = Integer.MAX_VALUE;
        }
        return persistentImageConsultationRepository.findAllByProjectAndUser(project.getId(), user.getId(), PageRequest.of(0, max, Sort.Direction.DESC, "created"));
    }

    public List<JsonObject> listImageConsultationByProjectAndUserWithDistinctImage(Project project, User user) {
        securityACLService.checkIsSameUserOrAdminContainer(project, user, currentUserService.getCurrentUser());
        List<Bson> requests = new ArrayList<>();
        List<JsonObject> data = new ArrayList<>();

        requests.add(match(eq("user", user.getId())));
        requests.add(match(eq("project", project.getId())));

        requests.add(group("$image", Accumulators.max("date", "$created"), Accumulators.first("time", "$time"), Accumulators.first("countCreatedAnnotations", "$countCreatedAnnotations")));
        requests.add(sort(descending("date")));

        MongoCollection<Document> persistentImageConsultation = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentImageConsultation");

        List<Document> results = persistentImageConsultation.aggregate(requests)
                .into(new ArrayList<>());

        LinkedHashMap<Long, ImageInstance> imageInstancesMap = new LinkedHashMap<>();

        for (Document result : results) {
            try {
                Long imageInstanceId = result.getLong("_id");
                ImageInstance image = imageInstancesMap.get(imageInstanceId);
                if (image == null) {
                    image = imageInstanceRepository.findById(imageInstanceId).orElse(null);
                    imageInstancesMap.put(imageInstanceId, image);
                }

                String filename;
                if (image != null) {
                    filename = image.getBlindInstanceFilename();
                } else {
                    filename = "Image " + imageInstanceId;
                }
                be.cytomine.utils.JsonObject jsonObject = new be.cytomine.utils.JsonObject();
                jsonObject.put("created", result.get("date"));
                jsonObject.put("image", imageInstanceId);
                jsonObject.put("user", result.get("user"));
                jsonObject.put("time", result.get("time"));
                if(image!=null) {
                    jsonObject.put("imageThumb", UrlApi.getImageInstanceThumbUrl(image.getId()));
                    jsonObject.put("project", image.getProject().getId());
                }
                jsonObject.put("imageName", filename);
                jsonObject.put("countCreatedAnnotations", result.get("countCreatedAnnotations"));
                data.add(jsonObject);
            } catch (CytomineException e) {
                //if user has data but has no access to picture,  ImageInstance.read will throw a forbiddenException
            }
        }
        data.sort(Comparator.comparing(o -> (Date) ((JsonObject) o).get("created")).reversed());
        return data;
    }

    public List<JsonObject> lastImageOfUsersByProject(Project project, List<Long> userIds, String sortProperty, String sortDirection, Long max, Long offset) {
        securityACLService.check(project, READ);
        List<JsonObject> data = new ArrayList<>();
        List<Bson> matchsFilters = new ArrayList<>();
        matchsFilters.add(match(eq("project", project.getId())));
        if (userIds != null) {
            matchsFilters.add(match(in("user", userIds)));
        }

        Bson sort = sort(sortDirection.equals("desc") ? descending(sortProperty) : ascending(sortProperty));

        Bson group = group("$user", Accumulators.max("created", "$created"), Accumulators.first("image", "$image"), Accumulators.first("imageName", "$imageName"), Accumulators.first("user", "$user"));

        Bson skip = skip(offset.intValue());

        List<Bson> requests = new ArrayList<>();
        requests.addAll(matchsFilters);
        requests.addAll(List.of(sort,group, sort, skip));

        if (max > 0) {
            requests.add(limit(max.intValue()));
        }

        MongoCollection<Document> persistentImageConsultation = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentImageConsultation");

        List<Document> results = persistentImageConsultation.aggregate(requests)
                .into(new ArrayList<>());

        ImageInstance image = null;
        String filename = "";

        for (Document result : results) {
            if (image == null) {
                image = imageInstanceRepository.findById(result.getLong("image")).orElse(null);
                if (image != null) {
                    filename = image.getBlindInstanceFilename();
                } else {
                    filename = "Image " + result.get("image");
                }
            }
            data.add(JsonObject.of("user", result.get("_id"), "created", result.get("created"), "image", result.get("image")
                    , "imageName", filename));
        }
        return data;
    }

    /**
     * return the last Image Of users in a Project. If a user (in the userIds array) doesn't have consulted an image yet, null values will be associated to the user id.
     */
    public List<JsonObject> lastImageOfGivenUsersByProject(Project project, List<Long> userIds, String sortProperty, String sortDirection, Long max, Long offset) {
        List<JsonObject> results = new ArrayList<>();

        AggregationResults queryResults = persistentImageConsultationRepository.retrieve(project.getId(), sortProperty, (sortDirection.equals("desc") ? -1 : 1));
        List aggregation = queryResults.getMappedResults();

        List<Long> connected = (List<Long>) aggregation.stream().map(x -> x instanceof LinkedHashMap ? (Long)((LinkedHashMap)x).get("user") : (Long)((PersistentImageConsultation)x).getUser()).distinct().collect(Collectors.toList());

        List<Long> unconnectedIds = new ArrayList<>(userIds);
        unconnectedIds.removeAll(connected);

        List<JsonObject> unconnected = unconnectedIds.stream().map(x -> JsonObject.of("user", (Object) x)).collect(Collectors.toList());

        if (max == 0) {
            max = unconnected.size() + connected.size() - offset;
        }

        if (sortDirection.equals("desc")) {
            //if o+l <= #connected ==> return connected with o et l
            // if o+l > #c c then return connected with o et l and append enough "nulls"

            if (offset < connected.size()) {
                results = lastImageOfUsersByProject(project, null, sortProperty, sortDirection, max, offset);
            }
            int maxOfUnconnected = (int) Math.max(max - results.size(), 0);
            int offsetOfUnconnected = (int) Math.max(offset - connected.size(), 0);
            if (maxOfUnconnected > 0) {
                results.addAll(unconnected.subList(offsetOfUnconnected, offsetOfUnconnected + maxOfUnconnected));
            }
        } else {
            if (offset + max <= unconnected.size()) {
                results = unconnected.subList(offset.intValue(), (int) (offset + max));
            } else if (offset + max > unconnected.size() && offset <= unconnected.size()) {
                results = unconnected.subList(offset.intValue(), unconnected.size());
                results.addAll(lastImageOfUsersByProject(project, null, sortProperty, sortDirection, max - (unconnected.size() - offset), 0L));
            } else {
                results.addAll(lastImageOfUsersByProject(project, null, sortProperty, sortDirection, max, offset - unconnected.size()));
            }
        }
        return results;
    }


    public List<JsonObject> getImagesOfUsersByProjectBetween(User user, Project project, Date after, Date before) {
        return getImagesOfUsersByProjectBetween(user.getId(), project.getId(), after, before);
    }

    public List<JsonObject> getImagesOfUsersByProjectBetween(Long userId, Long projectId, Date after, Date before) {
        List<JsonObject> data = new ArrayList<>();
        List<Bson> requests = new ArrayList<>();
        if (after != null) {
            requests.add(match(gte("created", after)));
        }
        if (before != null) {
            requests.add(match(lte("created", before)));
        }
        requests.add(match(eq("project", projectId)));
        requests.add(match(eq("user", userId)));
        requests.add(sort(descending("created")));

        MongoCollection<Document> persistentImageConsultation = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentImageConsultation");

        List<Document> results = persistentImageConsultation.aggregate(requests)
                .into(new ArrayList<>());

        LinkedHashMap<Long, ImageInstance> imageInstancesMap = new LinkedHashMap<>();

        for (Document result : results) {
            ImageInstance image = imageInstancesMap.get(result.getLong("image"));
            String filename = "";
            if (image == null) {
                image = imageInstanceRepository.findById(result.getLong("image")).orElse(null);
                if (image != null) {
                    imageInstancesMap.put(result.getLong("image"), image);
                }
                if (image != null) {
                    filename = image.getBlindInstanceFilename();
                } else {
                    filename = "Image " + result.get("image");
                }
            }
            data.add(JsonObject.of("user", result.get("user"), "created", result.get("created"), "image", result.get("image")
                    , "imageName", filename, "mode", result.get("mode")));
        }
        return data;

    }

    public List<JsonObject> resumeByUserAndProject(Long userId, Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        securityACLService.check(project, READ);

        List<Bson> requests = new ArrayList<>();
        requests.add(match(eq("project", projectId)));
        requests.add(match(eq("user", userId)));
        requests.add(sort(ascending("created")));
        requests.add(group(Document.parse("{project: '$project', user: '$user', image: '$image'}"),
                Accumulators.sum("time", "$time"),
                Accumulators.sum("frequency", 1),
                Accumulators.sum("countCreatedAnnotations", "$countCreatedAnnotations"),
                Accumulators.first("first", "$created"),
                Accumulators.last("last", "$created"),
                Accumulators.last("imageName", "$imageName"),
                Accumulators.last("imageThumb", "$imageThumb")));


        List<JsonObject> data = new ArrayList<>();

        MongoCollection<Document> persistentImageConsultation = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentImageConsultation");

        List<Document> results = persistentImageConsultation.aggregate(requests)
                .into(new ArrayList<>());

        LinkedHashMap<Long, ImageInstance> imageInstancesMap = new LinkedHashMap<>();

        for (Document result : results) {
            Long imageId = ((Document) result.get("_id")).getLong("image");

            ImageInstance image = imageInstancesMap.get(imageId);
            String filename = "";
            if (image == null) {
                image = imageInstanceRepository.findById(imageId).orElse(null);
                if (image != null) {
                    imageInstancesMap.put(imageId, image);
                }
                if (image != null) {
                    filename = image.getBlindInstanceFilename();
                } else {
                    filename = "Image " + imageId;
                }
            }

            JsonObject jsonObject = new JsonObject();
            jsonObject.put("project", projectId);
            jsonObject.put("user", userId);
            jsonObject.put("image", imageId);
            jsonObject.put("time", result.get("time"));
            jsonObject.put("countCreatedAnnotations", result.get("countCreatedAnnotations"));
            jsonObject.put("first", result.get("first"));
            jsonObject.put("last", result.get("last"));
            jsonObject.put("frequency", result.get("frequency"));
            jsonObject.put("imageName", filename);
            jsonObject.put("imageThumb", result.get("imageThumb"));
            data.add(jsonObject);
        }
        return data;
    }


    public Long countByProject(Project project, Long startDate, Long endDate) {
        if (startDate == null && endDate == null) {
            return persistentImageConsultationRepository.countByProject(project.getId());
        } else if (endDate == null) {
            return persistentImageConsultationRepository.countByProjectAndCreatedAfter(project.getId(), new Date(startDate));
        } else if (startDate == null) {
            return persistentImageConsultationRepository.countByProjectAndCreatedBefore(project.getId(), new Date(endDate));
        } else {
            return persistentImageConsultationRepository.countByProjectAndCreatedBetween(project.getId(), new Date(startDate), new Date(endDate));
        }
    }


    public List listLastOpened(Long max) {
        User user = (User)currentUserService.getCurrentUser();
        securityACLService.checkIsSameUser(user, currentUserService.getCurrentUser());

        List<Bson> requests = new ArrayList<>();
        requests.add(match(eq("user", user.getId())));
        requests.add(sort(ascending("created")));
        requests.add(group("$image",
                Accumulators.max("date", "$created")));
        requests.add(sort(descending("date")));
        requests.add(limit(max == null || max ==0 ? 5 : max.intValue()));

        List<JsonObject> data = new ArrayList<>();

        MongoCollection<Document> persistentImageConsultation = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentImageConsultation");


        List<Document> results = persistentImageConsultation.aggregate(requests)
                .into(new ArrayList<>());
        for (Document result : results) {
            try {
                ImageInstance imageInstance = imageInstanceService.find(result.getLong("_id"))
                        .orElseThrow(() -> new ObjectNotFoundException("ImageInstance", result.get("_id")));
                JsonObject jsonObject = new JsonObject();
                jsonObject.put("id", result.get("_id"));
                jsonObject.put("date", result.get("date"));
                jsonObject.put("thumb", UrlApi.getImageInstanceThumbUrl(imageInstance.getId()));
                jsonObject.put("instanceFilename", imageInstance.getBlindInstanceFilename());
                jsonObject.put("project", imageInstance.getProject().getId());
                data.add(jsonObject);
            } catch(CytomineException ex) {
                //if user has data but has no access to picture,  ImageInstance.read will throw a forbiddenException
            }

        }
        data.sort(Comparator.comparing(o -> (Date) ((JsonObject) o).get("date")).reversed());
        return data;
    }
}
