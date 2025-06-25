package be.cytomine.service.project;

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

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.domain.security.User;
import be.cytomine.dto.DatedCytomineDomain;
import be.cytomine.dto.NamedCytomineDomain;
import be.cytomine.dto.ProjectBounds;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.command.CommandHistoryRepository;
import be.cytomine.repository.command.CommandRepository;
import be.cytomine.repository.command.RedoStackItemRepository;
import be.cytomine.repository.command.UndoStackItemRepository;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.project.ProjectRepresentativeUserRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.ontology.AnnotationTermService;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.ontology.ReviewedAnnotationService;
import be.cytomine.service.search.ProjectSearchExtension;
import be.cytomine.service.security.UserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.*;
import be.cytomine.utils.filters.SQLSearchParameter;
import be.cytomine.utils.filters.SearchParameterEntry;
import be.cytomine.utils.filters.SearchParameterProcessed;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.service.social.ImageConsultationService.DATABASE_NAME;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Sorts.descending;
import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
public class ProjectService extends ModelService {

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private CommandHistoryRepository commandHistoryRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AnnotationTermService annotationTermService;

    @Autowired
    private ReviewedAnnotationService reviewedAnnotationService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectRepresentativeUserService projectRepresentativeUserService;

    @Autowired
    private AnnotationDomainRepository annotationDomainRepository;

    @Autowired
    private ImageInstanceRepository imageInstanceRepository;

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private ImageInstanceService imageInstanceService;

    @Autowired
    private UndoStackItemRepository undoStackItemRepository;

    @Autowired
    private RedoStackItemRepository redoStackItemRepository;

    @Autowired
    MongoClient mongoClient;

    @Autowired
    private CurrentRoleService currentRoleService;

    @Autowired
    private ProjectRepresentativeUserRepository projectRepresentativeUserRepository;

    @Autowired
    private RestTemplate restTemplate;

    public Project get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<Project> find(Long id) {
        Optional<Project> project = projectRepository.findById(id);
        project.ifPresent(value -> securityACLService.check(value, READ));
        return project;
    }

    public List<Project> readMany(Collection<Long> ids) {
        List<Project> projects = projectRepository.findAllById(ids);
        for (Project project : projects) {
            securityACLService.check(project, READ);
        }
        return projects;
    }

    public ProjectBounds computeBounds(Boolean withMembersCount) {
        User user = currentUserService.getCurrentUser();
        if(currentRoleService.isAdminByNow(user)) {
            //if user is admin, we print all available project
            user = null;
        } else {
            securityACLService.checkGuest(user);
        }
        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();
        projectSearchExtension.setWithMembersCount(withMembersCount);

        Page<JsonObject> results = this.list(user, projectSearchExtension, new ArrayList<>(), "created", "desc", 0L, 0L);
        ProjectBounds projectBounds = new ProjectBounds();
        results.forEach(projectBounds::submit);
        return projectBounds;
    }

    /**
     * List last project opened by user
     * If the user has less than "max" project opened, add last created project to complete list
     */
    public List<Map<String, Object>> listLastOpened(User user, Long max) {
        securityACLService.checkIsSameUser(user,currentUserService.getCurrentUser());
        if (max == null || max == 0L) {
            max = 5L;
        }
        List<Bson> requests = new ArrayList<>();
        requests.add(match(eq("user", user.getId())));
        requests.add(group("$project", Accumulators.max("date", "$created")));
        requests.add(sort(descending("date")));
        requests.add(limit(max.intValue()));

        MongoCollection<Document> persistentImageConsultation = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentProjectConnection");

        List<Document> results = persistentImageConsultation.aggregate(requests)
                .into(new ArrayList<>());


        List<Map<String, Object>> data = results.stream().map(x -> JsonObject.of("id", x.get("_id"), "date", x.get("date"), "opened", true))
                .collect(Collectors.toList());

        if (data.size()<max) {
            //user has open less than max project, so we add last created project
            List<DatedCytomineDomain> unopened = data.isEmpty() ?
                    projectRepository.listLastCreated() :
                    projectRepository.listLastCreated(data.stream().map(x -> (Long)x.get("id")).collect(Collectors.toList()));
            for (DatedCytomineDomain datedCytomineDomain : unopened) {
                data.add(JsonObject.of("id", datedCytomineDomain.getId(), "date", datedCytomineDomain.getDate(), "opened", false));
            }

        }
        data.sort(Comparator.comparing(o -> ((Map<String, Object>) o).get("date")!=null ? (Date) ((Map<String, Object>) o).get("date") : new Date(0)).reversed());

        data = data.subList(0, Math.min(data.size(), max.intValue()));
        return data;
    }

    public List<Project> listForCurrentUser() {
        return projectRepository.findAllProjectForUser(currentUserService.getCurrentUsername());
    }

    public Page<JsonObject> list(User user, ProjectSearchExtension projectSearchExtension, List<SearchParameterEntry> searchParameters, String sortColumn, String sortDirection, Long max, Long offset) {
        if (user==null) {
            securityACLService.checkAdmin(currentUserService.getCurrentUser());
        } else {
            securityACLService.checkGuest(user);
        }

        for (SearchParameterEntry parameter : searchParameters){
            if(parameter.getProperty().equals("numberOfImages")){
                parameter.setProperty("countImages");
            }
            if(parameter.getProperty().equals("numberOfJobAnnotations")) {
                parameter.setProperty("countJobAnnotations");
            }
            if(parameter.getProperty().equals("numberOfReviewedAnnotations")) {
                parameter.setProperty("countReviewedAnnotations");
            }
            if(parameter.getProperty().equals("numberOfAnnotations")) {
                parameter.setProperty("countAnnotations");
            }
            if(parameter.getProperty().equals("ontology")) {
                parameter.setProperty("ontology_id");
            }
        }
        if (sortColumn.equals("lastActivity") && !projectSearchExtension.isWithLastActivity()) {
            throw new WrongArgumentException("Cannot sort on lastActivity without argument withLastActivity");
        }
        if (sortColumn.equals("membersCount") && !projectSearchExtension.isWithMembersCount()) {
            throw new WrongArgumentException("Cannot sort on membersCount without argument withMembersCount");
        }

        log.debug("searchParameters:");
        searchParameters.stream().map(x -> x.toString()).forEach(log::debug);

        List<SearchParameterEntry> validParameters = SQLSearchParameter.getDomainAssociatedSearchParameters(Project.class, searchParameters, getEntityManager());

        log.debug("validParameters:");
        validParameters.stream().map(x -> x.toString()).forEach(log::debug);

        validParameters.forEach(searchParameterEntry -> searchParameterEntry.setProperty("p."+searchParameterEntry.getProperty()));

        for (SearchParameterEntry parameter : searchParameters){
            String property;
            switch(parameter.getProperty()) {
                case "ontology_id" :
                    property = "ontology.id";
                    parameter.setValue(SQLSearchParameter.convertSearchParameter(Long.class, parameter.getValue(), getEntityManager()));
                    validParameters.add(new SearchParameterEntry(property, parameter.getOperation(), parameter.getValue()));
                    break;
                case "membersCount" :
                    property = "members.member_count";
                    parameter.setValue(SQLSearchParameter.convertSearchParameter(Long.class, parameter.getValue(), getEntityManager()));
                    validParameters.add(new SearchParameterEntry(property, parameter.getOperation(), parameter.getValue()));
                    break;
                case "tag" :
                    property = "t.tag_id";
                    parameter.setValue(SQLSearchParameter.convertSearchParameter(Long.class, parameter.getValue(), getEntityManager()));
                    validParameters.add(new SearchParameterEntry(property, parameter.getOperation(), parameter.getValue()));
                    break;
                default:
                    continue;
            }
        }

        SearchParameterProcessed sqlSearchConditions = SQLSearchParameter.searchParametersToSQLConstraints(validParameters);
        log.debug("sqlSearchConditions:");
        sqlSearchConditions.getData().stream().map(x -> x.toString()).forEach(log::debug);
        log.debug("sqlSearchConditions.params:");
        sqlSearchConditions.getSqlParameters().entrySet().stream().map(Object::toString).forEach(log::debug);

        String project = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith("p.")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));
        String ontology = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith("ontology.")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));
        String members = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith("members.")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));
        String tags = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith("t.")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));

        if (!members.isBlank() && !projectSearchExtension.isWithMembersCount()) {
            throw new WrongArgumentException("Cannot search on members attributes without argument withMembersCount");
        }

        String select, from, where, search, sort;
        String request;

        if (user!=null) {
            select = "SELECT DISTINCT p.* ";
            from = "FROM project p " +
                    "JOIN acl_object_identity as aclObjectId ON aclObjectId.object_id_identity = p.id " +
                    "JOIN acl_entry as aclEntry ON aclEntry.acl_object_identity = aclObjectId.id " +
                    "JOIN acl_sid as aclSid ON aclEntry.sid = aclSid.id ";
            where = "WHERE aclSid.sid like '"+user.getUsername()+"' ";
        }
        else {
            select = "SELECT DISTINCT(p.id) as distinctId, p.* ";
            from = "FROM project p ";
            where = "WHERE true ";
        }
        select += ", ontology.name as ontology_name, ontology.id as ontology ";
        from += "LEFT OUTER JOIN ontology ON p.ontology_id = ontology.id ";

        search = "";sort = "";
        if(!project.isBlank()){
            search +=" AND ";
            search += project;
        }

        if(!ontology.isBlank()){
            search +=" AND ";
            search += ontology;
        }

        if(!tags.isBlank()){
            from += "LEFT OUTER JOIN tag_domain_association t ON p.id = t.domain_ident AND t.domain_class_name = 'be.cytomine.domain.project.Project' ";
            search +=" AND ";
            search += tags;
        }


        if(projectSearchExtension.isWithLastActivity()) {
            select += ", activities.max_date ";
            from += "LEFT OUTER JOIN " +
                    "( SELECT  project_id, MAX(created) max_date " +
                    "  FROM command_history " +
                    "  GROUP BY project_id " +
                    ") activities ON p.id = activities.project_id ";
        }
        if(projectSearchExtension.isWithMembersCount()) {
            select += ", members.member_count ";
            from += "LEFT OUTER JOIN " +
                    " ( SELECT aclObjectId.object_id_identity as project_id, COUNT(DISTINCT secUser.id) as member_count " +
                    "   FROM acl_object_identity as aclObjectId, acl_entry as aclEntry, acl_sid as aclSid, sec_user as secUser " +
                    "   WHERE aclEntry.acl_object_identity = aclObjectId.id and aclEntry.sid = aclSid.id and aclSid.sid = secUser.username" +
                    "   GROUP BY aclObjectId.object_id_identity " +
                    ") members ON p.id = members.project_id ";

            if(!members.isBlank()){
                search +=" AND ";
                search += members;
            }
        }
        if (projectSearchExtension.isWithDescription()) {
            select += ", d.data as description ";
            from += "LEFT OUTER JOIN description d ON d.domain_ident = p.id ";
        }
        if(projectSearchExtension.isWithCurrentUserRoles()) {
            User currentUser = currentUserService.getCurrentUser(); // cannot use user param because it is set to null if user connected as admin
            select += ", (admin_project.id IS NOT NULL) AS is_admin, (repr.id IS NOT NULL) AS is_representative ";
            from += "LEFT OUTER JOIN admin_project " +
                    "ON admin_project.id = p.id AND admin_project.user_id = " + currentUser.getId() + " " +
                    "LEFT OUTER JOIN project_representative_user repr " +
                    "ON repr.project_id = p.id AND repr.user_id = " + currentUser.getId() + " ";

            SearchParameterEntry searchedRole = searchParameters.stream().filter(x -> x.getProperty().equals("currentUserRole")).findFirst().orElse(null);
            if(searchedRole!=null) {
                List<String> value = (searchedRole.getValue() instanceof String? List.of((String)searchedRole.getValue()) : ((List)searchedRole.getValue()));
                if(value.contains("manager") && value.contains("contributor")){} // nothing because null or not null
                else if(value.contains("manager")) {
                    search += " AND admin_project.id IS NOT NULL  ";
                }
                else if(value.contains("contributor")) {
                    search += " AND admin_project.id IS NULL  ";
                }
            }

        }


        switch(sortColumn) {
            case "currentUserRole" :
                if(projectSearchExtension.isWithCurrentUserRoles()) {
                    sortColumn="is_representative "+((sortDirection.equals("desc")) ? " DESC " : " ASC ")+", is_admin";
                }
                break;
            case "membersCount" :
                if(projectSearchExtension.isWithMembersCount()) {
                    sortColumn="members.member_count";
                }
                break;
            case "lastActivity" :
                if(projectSearchExtension.isWithLastActivity()) {
                    sortColumn="activities.max_date";
                }
                break;
            case "name":
            case "numberOfImages":
            case "numberOfAnnotations":
            case "numberOfJobAnnotations":
            case "numberOfReviewedAnnotations":
                String regex = "([a-z])([A-Z]+)";
                String replacement = "$1_$2";
                sortColumn ="p."+sortColumn.replaceAll("numberOf", "count").replaceAll(regex, replacement).toLowerCase();
                break;
        }

        sort = " ORDER BY "+sortColumn;
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC ";
        sort += (sortDirection.equals("desc")) ? " NULLS LAST " : " NULLS FIRST ";

        request = select + from + where + search + sort;

        if (max > 0) {
            request += " LIMIT " + max;
        }
        if (offset > 0) {
            request += " OFFSET " + offset;
        }

        log.debug(request);
        Query query = getEntityManager().createNativeQuery(request, Tuple.class);
        Map<String, Object> mapParams = sqlSearchConditions.getSqlParameters();
        for (Map.Entry<String, Object> entry : mapParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        List<Tuple> resultList = query.getResultList();
        List<JsonObject> results = new ArrayList<>();
        for (Tuple rowResult : resultList) {
            JsonObject result = new JsonObject();
            for (TupleElement<?> element : rowResult.getElements()) {
                Object value = rowResult.get(element.getAlias());
                if (value instanceof BigInteger) {
                    value = ((BigInteger)value).longValue();
                }
                String alias = SQLUtils.toCamelCase(element.getAlias());
                result.put(alias, value);
            }
            result.computeIfPresent("created", (k, v) -> ((Date)v).getTime());
            result.computeIfPresent("updated", (k, v) -> ((Date)v).getTime());
            Ontology eagerOntology = new Ontology();
            eagerOntology.setId((Long)result.get("ontology"));
            eagerOntology.setName((String)result.get("ontologyName"));
            result.put("ontology", eagerOntology);
            JsonObject object = Project.getDataFromDomain(new Project().buildDomainFromJson(result, getEntityManager()));
            object.put("numberOfImages", result.get("countImages"));
            object.put("numberOfAnnotations", result.get("countAnnotations"));
            object.put("numberOfJobAnnotations", result.get("countJobAnnotations"));
            object.put("numberOfReviewedAnnotations", result.get("countReviewedAnnotations"));

            if(projectSearchExtension.isWithLastActivity()) {
                object.put("lastActivity", result.get("maxDate"));
            }
            if(projectSearchExtension.isWithMembersCount()) {
                object.put("membersCount", result.get("memberCount")==null ? 0 : result.get("memberCount"));
            }
            if (projectSearchExtension.isWithDescription()) {
                object.put("description", result.get("description")==null ? "" : result.get("description"));
            }
            if(projectSearchExtension.isWithCurrentUserRoles()) {
                object.put("currentUserRoles", JsonObject.of("admin", result.get("isAdmin"), "representative", result.get("isRepresentative")));
            }
            results.add(object);
        }
        request = "SELECT COUNT(DISTINCT p.id) " + from + where + search;
        query = getEntityManager().createNativeQuery(request);
        for (Map.Entry<String, Object> entry : mapParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        long count = (Long)query.getResultList().get(0);
        Page<JsonObject> page = PageUtils.buildPageFromPageResults(results, max, offset, count);
        return page;

    }

    public List<JsonObject> findCommandHistory(List<Project> projects, Long user, Long max, Long offset,
                                               Boolean fullData, Long startDate, Long endDate) {

        if (max == 0) {
            max = Long.MAX_VALUE;
        }

        if (projects != null && projects.isEmpty()) {
            return new ArrayList<>();
        }

        String select = "SELECT ch.id as id, ch.created as created, ch.message as message, " +
                "ch.prefix_action as prefixAction, ch.user_id as user, ch.project_id as project ";
        String from = "FROM command_history ch ";
        String where = "WHERE true " +
                (projects!=null? "AND ch.project_id IN ("+projects.stream().map(x -> x.getId().toString()).collect(Collectors.joining(","))+") " : " ") +
                (user!=null? "AND ch.user_id =  "+user+" " : " ") +
                (startDate!=null ? "AND ch.created > '"+new Date(startDate)+"' " : "") +
                (endDate!=null ? "AND ch.created < '"+new Date(endDate)+"' " : "");
        String orderBy = "ORDER BY ch.created desc LIMIT "+max+" OFFSET " + offset;

        if(fullData) {
            select += ", c.data as data,c.service_name as serviceName, " +
                    "c.class as className, c.action_message as actionMessage, u.username as username ";
            from += "LEFT JOIN command c ON ch.command_id = c.id " +
                    "LEFT JOIN sec_user u ON u.id = ch.user_id ";
        }

        List<JsonObject> data = new ArrayList<>();
        Long start = System.currentTimeMillis();

        Query nativeQuery = getEntityManager().createNativeQuery(select + from + where + orderBy, Tuple.class);
        List<Tuple> resultList = nativeQuery.getResultList();
        for (Tuple tuple : resultList) {
            if(data.isEmpty()) {
                start = System.currentTimeMillis();
            }
            JsonObject jsonObject = JsonObject.of(
                  "id", tuple.get("id"),
                  "created", tuple.get("created"),
                  "message", tuple.get("message"),
                  "prefix", tuple.get("prefixAction"),
                  "prefixAction", tuple.get("prefixAction"),
                  "user", tuple.get("user"),
                  "project", tuple.get("project"));

            if(fullData) {
                jsonObject.put("data", tuple.get("data"));
                jsonObject.put("serviceName", tuple.get("serviceName"));
                jsonObject.put("className", tuple.get("className"));
                jsonObject.put("action", tuple.get("actionMessage") + " by " +  tuple.get("username"));
            }
            data.add(jsonObject);
        }
        return data;
    }

    public List<Project> listByOntology(Ontology ontology) {
        if (currentRoleService.isAdminByNow(currentUserService.getCurrentUser())) {
            return projectRepository.findAllByOntology(ontology);
        }
        return projectRepository.findAllProjectForUserByOntology(currentUserService.getCurrentUsername(),ontology);
    }

    public List<CommandHistory> lastAction(Project project, int max) {
        securityACLService.check(project, READ);
        return commandHistoryRepository.findAllByProject(project, PageRequest.of(0, max, Sort.by("created").descending()));
    }

    public List<NamedCytomineDomain> listByCreator(User user) {
        securityACLService.checkIsSameUser(user,currentUserService.getCurrentUser());
        return projectRepository.listByCreator(user);
    }

    public List<NamedCytomineDomain> listByAdmin(User user) {
        securityACLService.checkIsSameUser(user,currentUserService.getCurrentUser());
        return projectRepository.listByAdmin(user);
    }

    public List<NamedCytomineDomain> listByUser(User user) {
        securityACLService.checkIsSameUser(user,currentUserService.getCurrentUser());
        return projectRepository.listByUser(user);
    }

    @Override
    public CommandResponse add(JsonObject jsonObject) {
        return add(jsonObject, null);
    }

    @Override
    public CommandResponse add(JsonObject jsonObject, Task task) {
        taskService.updateTask(task,5,"Start creating project " + jsonObject.getJSONAttrStr("name"));
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

        if(jsonObject.get("ontology")!=null) {
            securityACLService.check(jsonObject.getJSONAttrLong("ontology"),Ontology.class, READ);
        }

        taskService.updateTask(task,10,"Check retrieval consistency");
        CommandResponse commandResponse = executeCommand(new AddCommand(currentUser), null, jsonObject);
        Project project = (Project)commandResponse.getObject();
        taskService.updateTask(task,20,"Project " +project.getName()+ " created");

        log.info("project=" + project + " json.users=" + jsonObject.get("users") + " json.admins=" + jsonObject.get("admins"));
        int progress = 20;

        List<Long> users = jsonObject.getJSONAttrListLong("users", new ArrayList<>());
        List<Long> admins = jsonObject.getJSONAttrListLong("admins", new ArrayList<>());

        users.addAll(admins);
        users = users.stream().distinct().collect(Collectors.toList());

        for (Long userId : users) {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isPresent()) {
                log.info("addUserToProject project="+project.getId()+" user="+optionalUser.get().getId());
                userService.addUserToProject(optionalUser.get(), project, false);
                progress = progress + (40/users.size());
                taskService.updateTask(task,Math.min(100,progress),"User "+optionalUser.get().getUsername()+" added as User");
            }
        }

        for (Long userId : admins) {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isPresent() && !Objects.equals(optionalUser.get().getId(), currentUserService.getCurrentUser().getId())) {
                // current user is already in project
                log.info("addUserToProject (admin) project="+project.getId()+" user="+optionalUser.get().getId());
                userService.addUserToProject(optionalUser.get(), project, true);
                progress = progress + (40/admins.size());
                taskService.updateTask(task,Math.min(100,progress),"User "+optionalUser.get().getUsername()+" added as Admin");
            }
        }

        return commandResponse;
    }

    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        return update(domain, jsonNewData, transaction, null);
    }

    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction, Task task) {
        Project project = (Project)domain;
        taskService.updateTask(task,5,"Start editing project " + project.getName());
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.check(project.container(),WRITE);
        Ontology ontology = project.getOntology();


        if(ontology!=null && !Objects.equals(ontology.getId(), jsonNewData.getJSONAttrLong("ontology", 0L))){
            boolean deleteTerms = jsonNewData.getJSONAttrBoolean("forceOntologyUpdate", false);
            long associatedTermsCount;
            long userAssociatedTermsCount = 0L;
            long algoAssociatedTermsCount = 0L;
            long reviewedAssociatedTermsCount = 0L;
            if(!deleteTerms) {
                userAssociatedTermsCount += annotationTermService.list(project).size();
            }
            algoAssociatedTermsCount += 0;
            reviewedAssociatedTermsCount += reviewedAnnotationService.countByProjectAndWithTerms(project);
            associatedTermsCount = userAssociatedTermsCount + algoAssociatedTermsCount + reviewedAssociatedTermsCount;

            if(associatedTermsCount > 0){
                String message = "This project has " + associatedTermsCount + " associated terms: ";
                if(!deleteTerms) {
                    message += userAssociatedTermsCount + " from project members, ";
                }
                message += algoAssociatedTermsCount + " from jobs and ";
                message += reviewedAssociatedTermsCount + " reviewed. ";
                message += "The ontology cannot be updated.";
                throw new ForbiddenException(message, Map.of(
                        "userAssociatedTermsCount", userAssociatedTermsCount,
                        "algoAssociatedTermsCount", algoAssociatedTermsCount,
                        "reviewedAssociatedTermsCount", reviewedAssociatedTermsCount));
            }
            if(deleteTerms) {
                for (AnnotationTerm annotationTerm : annotationTermService.list(project)) {
                    annotationTermService.delete(annotationTerm, transaction, task, false);
                }
            }
        }


        CommandResponse commandResponse = executeCommand(new EditCommand(currentUser, transaction), domain, jsonNewData);
        project = (Project) commandResponse.getObject();

        taskService.updateTask(task,20,"Project "+project.getName()+" edited");

        List<Long> users = jsonNewData.getJSONAttrListLong("users", null);
        List<Long> admins = jsonNewData.getJSONAttrListLong("admins", null);
        List<Long> representatives = jsonNewData.getJSONAttrListLong("representatives", null);



        if(users!=null) {
            List<Long> projectOldUsers = userService.listUsers(project).stream().map(CytomineDomain::getId).sorted().collect(Collectors.toList()); //[a,b,c]
            List<Long> projectNewUsers = users.stream().sorted().collect(Collectors.toList()); //[a,b,x]
            List<Long> nextAdmins;
            if(admins!=null) {
                nextAdmins = admins;
            } else {
                nextAdmins = userService.listAdmins(project).stream().map(CytomineDomain::getId).sorted().collect(Collectors.toList()); //[a,b,c]
            }
            projectNewUsers.addAll(nextAdmins);  //add admin as user too
            projectNewUsers.add(currentUser.getId());
            projectNewUsers = projectNewUsers.stream().distinct().collect(Collectors.toList());
            log.info("projectOldUsers=" + projectOldUsers);
            log.info("projectNewUsers=" + projectNewUsers);
            changeProjectUser(project,projectNewUsers,projectOldUsers,false,task,20);
        }


        if(admins!=null) {
            List<Long> projectOldAdmins = userService.listAdmins(project).stream().map(CytomineDomain::getId).sorted().collect(Collectors.toList()); //[a,b,c]
            List<Long> projectNewAdmins = admins.stream().sorted().collect(Collectors.toList()); //[a,b,x]
            projectNewAdmins.add(currentUser.getId());
            projectNewAdmins = projectNewAdmins.stream().distinct().collect(Collectors.toList());
            log.info("projectOldAdmins=" + projectOldAdmins);
            log.info("projectNewAdmins=" + projectNewAdmins);
            changeProjectUser(project,projectNewAdmins,projectOldAdmins,true,task,60);
        }

        // here, an empty array is a valid argument
        if(representatives != null) {
            List<Long> projectOldReprs = projectRepresentativeUserService.listByProject(project).stream().map (x -> x.getUser().getId()).sorted().collect(Collectors.toList()); //[a,b,c]
            List<Long> projectNewReprs = representatives.stream().sorted().distinct().collect(Collectors.toList()); //[a,b,x]
            log.info("projectOldReprs="+projectOldReprs);
            log.info("projectNewReprs="+projectNewReprs);
            List<Long> projectAddReprs = new ArrayList<>(projectNewReprs);
            projectAddReprs.removeAll(projectOldReprs);
            List<Long> projectDeleteReprs = new ArrayList<>(projectOldReprs);
            projectDeleteReprs.removeAll(projectNewReprs);

            log.info("projectAddUser="+projectAddReprs);
            log.info("projectDeleteUser="+projectDeleteReprs);

            for (Long projectAddRepr : projectAddReprs) {
                Optional<User> optionalUser = userRepository.findById(projectAddRepr);
                if (optionalUser.isPresent()) {
                    log.info("projectAddReprs project="+project+" user="+ optionalUser.get().getId());
                    ProjectRepresentativeUser pru = new ProjectRepresentativeUser();
                    pru.setProject(project);
                    pru.setUser(optionalUser.get());
                    projectRepresentativeUserService.add(pru.toJsonObject());
                }
            }

            for (Long projectDeleteRepr : projectDeleteReprs) {
                Optional<User> optionalUser = userRepository.findById(projectDeleteRepr);
                if (optionalUser.isPresent()) {
                    log.info("projectDeleteReprs project="+project+" user="+ optionalUser.get().getId());
                    Optional<ProjectRepresentativeUser> repr = projectRepresentativeUserService.find(project, optionalUser.get());
                    repr.ifPresent(x -> projectRepresentativeUserService.delete(x, transaction, task, false));
                }
            }
        }
        if(ontology!=null && !Objects.equals(ontology.getId(), jsonNewData.getJSONAttrLong("ontology"))){
            ontologyService.determineRightsForUsers(ontology, userService.listUsers(project));
            if(project.getOntology()!=null) {
                ontologyService.determineRightsForUsers(project.getOntology(), userService.listUsers(project));
            }
        }
        return commandResponse;

    }


    private void changeProjectUser(Project project, List<Long> projectNewUsers, List<Long> projectOldUsers, boolean admin, Task task, int progressStart) {
        int progress = progressStart;
        List<Long> projectAddUser = new ArrayList<>(projectNewUsers);
        projectAddUser.removeAll(projectOldUsers);

        List<Long> projectDeleteUser = new ArrayList<>(projectOldUsers);
        projectDeleteUser.removeAll(projectNewUsers);

        log.info("projectAddUser="+projectAddUser);
        log.info("projectDeleteUser="+projectDeleteUser);

        for (Long userId : projectAddUser) {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isPresent()) {
                log.info("addUserToProject project="+project.getId()+" user="+optionalUser.get().getId());
                userService.addUserToProject(optionalUser.get(), project, admin);
                progress = progress + (40/projectAddUser.size());
                taskService.updateTask(task,Math.min(100,progress),"User "+optionalUser.get().getUsername()+" added as " + (admin? "Admin" : "User"));
            }
        }

        for (Long userId : projectDeleteUser) {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isPresent()) {
                log.info("projectDeleteUser project="+project.getId()+" user="+optionalUser.get().getId());
                userService.deleteUserFromProject(optionalUser.get(), project, admin);
                log.info("changeProjectUser " + permissionService.hasACLPermission(project, optionalUser.get().getUsername(), ADMINISTRATION));
                progress = progress + (40/projectAddUser.size());
                taskService.updateTask(task,Math.min(100,progress),"User "+optionalUser.get().getUsername()+" removed as " + (admin? "Admin" : "User"));
            }
        }
    }

    public List<Long> getActiveProjects() {
        Date xSecondAgo = DateUtils.addSeconds(new Date(), -120);
        List<Bson> requests = new ArrayList<>();
        requests.add(match(gte("created", xSecondAgo)));
        requests.add(group("$project"));
        MongoCollection<Document> persistentImageConsultation = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentProjectConnection");
        List<Document> results = persistentImageConsultation.aggregate(requests)
                .into(new ArrayList<>());
        return results.stream().map(x -> x.getLong("_id")).collect(Collectors.toList());
    }

    public List<JsonObject> getActiveProjectsWithNumberOfUsers() {
        Date xSecondAgo = DateUtils.addSeconds(new Date(), -120);
        List<Bson> requests = new ArrayList<>();
        requests.add(match(gte("created", xSecondAgo)));
        requests.add(group(Document.parse("{project: '$project', user: '$user'}")));
        requests.add(group("$_id.project", Accumulators.sum("users", 1)));

        MongoCollection<Document> persistentImageConsultation = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentProjectConnection");
        List<Document> results = persistentImageConsultation.aggregate(requests)
                .into(new ArrayList<>());

        List<JsonObject> tmp = results.stream().map(x -> JsonObject.of("project", x.get("_id"), "users", x.get("users"))).collect(Collectors.toList());

        List<Project> projects = projectRepository.findAllByIdIn(tmp.stream().map(x-> (Long)x.get("project")).collect(Collectors.toList()));

        List<JsonObject> data = new ArrayList<>();
        for (Project project : projects) {
            JsonObject jsonObject = JsonObject.of("project", Project.getDataFromDomain(project));
            Optional<JsonObject> optEntry = tmp.stream().filter(entry -> Objects.equals(project.getId(), entry.getJSONAttrLong("project"))).findFirst();
            jsonObject.put("users", optEntry.orElse(new JsonObject()).get("users"));
            data.add(jsonObject);
        }
        return data;
    }


    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(),ADMINISTRATION);
        securityACLService.checkIsNotReadOnly(domain.container());
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        log.info("Add permission on " + domain + " to " + currentUserService.getCurrentUsername());
        if(!permissionService.hasACLPermission(domain, READ)) {
            log.info("force to put it in list");
            permissionService.addPermission(domain, currentUserService.getCurrentUsername(), BasePermission.READ);
        }
        if(!permissionService.hasACLPermission(domain, ADMINISTRATION)) {
            log.info("force to put it in list");
            permissionService.addPermission(domain, currentUserService.getCurrentUsername(), BasePermission.ADMINISTRATION);
        }

        if (projectRepresentativeUserService.find((Project)domain, (User)currentUserService.getCurrentUser()).isEmpty()) {
            log.info("add creator "+currentUserService.getCurrentUsername()+" as representative for project " + domain);
            ProjectRepresentativeUser pru = new ProjectRepresentativeUser();
            pru.setProject((Project) domain);
            pru.setUser((User) currentUserService.getCurrentUser());
            projectRepresentativeUserService.add(pru.toJsonObject());
        }

    }


    protected void beforeUpdate(CytomineDomain domain) {
        Project project = (Project)domain;
        project.setCountAnnotations(annotationDomainRepository.countAllUserAnnotationAndProject(domain.getId()));
//        project.setCountJobAnnotations(annotationDomainRepository.countAllAlgoAnnotationAndProject(domain.getId()));
        project.setCountReviewedAnnotations(annotationDomainRepository.countAllReviewedAnnotationAndProject(domain.getId()));
        project.setCountImages(imageInstanceRepository.countAllByProject(project));
    }

    protected void beforeDelete(CytomineDomain domain) {
        Project project = (Project)domain;
        commandHistoryRepository.deleteAllByProject(project);
        undoStackItemRepository.deleteAllByCommand_Project(project);
        redoStackItemRepository.deleteAllByCommand_Project(project);
        commandRepository.deleteAllByProject(project);
    }

    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((Project)domain).getName());
    }


    public void checkDoNotAlreadyExist(CytomineDomain domain){
        Project project = (Project)domain;
        if(project!=null && project.getName()!=null) {
            if(projectRepository.findByName(project.getName()).stream().anyMatch(x -> !Objects.equals(x.getId(), project.getId())))  {
                throw new AlreadyExistException("Project " + project.getName() + " already exist!");
            }
        }
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentImageInstance((Project) domain, transaction, task);
        deleteDependentRepresentativeUser((Project) domain, transaction, task);
        deleteDependentMetadata(domain, transaction, task);
    }

    private void deleteDependentRepresentativeUser(Project domain, Transaction transaction, Task task) {
        projectRepresentativeUserRepository.deleteAll(projectRepresentativeUserService.listByProject(domain));
    }

    private void deleteDependentImageInstance(Project project, Transaction transaction, Task task) {
         taskService.updateTask(task,(task!=null? "Delete " +imageInstanceRepository.countAllByProject(project)+ " images":""));
        for (ImageInstance imageInstance : imageInstanceRepository.findAllByProject(project)) {
            imageInstanceService.delete(imageInstance, transaction, task , false);
        }

    }

    @Override
    public Class currentDomain() {
        return Project.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Project().buildDomainFromJson(json, getEntityManager());
    }



}
