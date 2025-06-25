package be.cytomine.service.image;

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
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.*;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.AbstractImageRepository;
import be.cytomine.repository.image.AbstractSliceRepository;
import be.cytomine.repository.image.CompanionFileRepository;
import be.cytomine.repository.image.UploadedFileRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.*;
import be.cytomine.utils.filters.SQLSearchParameter;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import be.cytomine.utils.filters.SearchParameterProcessed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.transaction.Transactional;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
public class UploadedFileService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private AbstractImageRepository abstractImageRepository;

    @Autowired
    private AbstractSliceRepository abstractSliceRepository;

    @Autowired
    private AbstractImageService abstractImageService;

    @Autowired
    private AbstractSliceService abstractSliceService;

    @Autowired
    private CompanionFileService companionFileService;

    @Autowired
    private CompanionFileRepository companionFileRepository;

    @Autowired
    private TaskService taskService;


    @Override
    public Class currentDomain() {
        return UploadedFile.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new UploadedFile().buildDomainFromJson(json, getEntityManager());
    }


    public Page<UploadedFile> list(Pageable pageable) {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return uploadedFileRepository.search(null, null, null, null, pageable);
    }

    public Page<UploadedFile> list(User user, Long parentId, Boolean onlyRoot, Pageable pageable) {
        securityACLService.checkIsSameUser(user, currentUserService.getCurrentUser());
        List<Storage> storages = securityACLService.getStorageList(currentUserService.getCurrentUser(), false);
        return uploadedFileRepository.search(user.getId(), parentId, onlyRoot, DomainUtils.extractIds(storages), pageable);
    }

    public List<Map<String, Object>> list(List<SearchParameterEntry> searchParameters, String sortedProperty, String sortDirection, Boolean withTreeDetails) {

        // authorization check is done in the sql request

        searchParameters.stream().filter(x -> x.getProperty().equals("storage")).findFirst().ifPresent(x -> x.setOperation(SearchOperation.in));
        searchParameters.stream().filter(x -> x.getProperty().equals("user")).findFirst().ifPresent(x -> x.setOperation(SearchOperation.in));

        List<SearchParameterEntry> validatedSearchParameters = SQLSearchParameter.getDomainAssociatedSearchParameters(UploadedFile.class, searchParameters, getEntityManager());

        for (SearchParameterEntry validatedSearchParameter : validatedSearchParameters) {
            if (!validatedSearchParameter.getProperty().contains(".")) {
                validatedSearchParameter.setProperty("uf." + validatedSearchParameter.getProperty());
            }
            if (validatedSearchParameter.getValue() instanceof CytomineDomain) {
                validatedSearchParameter.setValue(((CytomineDomain) validatedSearchParameter.getValue()).getId());
                validatedSearchParameter.setProperty(validatedSearchParameter.getProperty() + "_id");
            }
            if (validatedSearchParameter.getValue() instanceof List) {
                if (((List) validatedSearchParameter.getValue()).size() > 0 && ((List) validatedSearchParameter.getValue()).get(0) instanceof CytomineDomain) {
                    List<Long> list = new ArrayList<>();
                    for (Object o : ((List) validatedSearchParameter.getValue())) {
                        list.add(((CytomineDomain) o).getId());
                    }
                    validatedSearchParameter.setValue(list);
                    validatedSearchParameter.setProperty(validatedSearchParameter.getProperty() + "_id");
                }

            }
        }

        validatedSearchParameters.addAll(
            SQLSearchParameter
                .getDomainAssociatedSearchParameters(AbstractImage.class, searchParameters, getEntityManager())
                .stream()
                .filter(x -> !x.getProperty().equals("original_filename"))
                .map(x -> new SearchParameterEntry("ai." + x.getProperty(), x.getOperation(), x.getValue()))
                .toList()
        );

        SearchParameterProcessed sqlSearchConditions = SQLSearchParameter.searchParametersToSQLConstraints(validatedSearchParameters);
        String search = sqlSearchConditions.getData().stream().map(SearchParameterEntry::getSql).collect(Collectors.joining(" AND "));

        String sort = "";
        if (List.of("content_type", "id", "created", "filename", "originalFilename", "size", "status").contains(sortedProperty)) {
            sort = "uf." + SQLUtils.toSnakeCase(sortedProperty);
        } else if (withTreeDetails && sortedProperty.equals("globalSize")) {
            sort = "COALESCE(SUM(DISTINCT tree.size),0)+uf.size";
        } else {
            sort = "uf.created ";
        }
        sort = " ORDER BY " + sort;
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC ";


        String treeSelect = "";
        String treeJoin = "";
        if (withTreeDetails) {
            treeSelect += "COUNT(DISTINCT tree.id) AS nb_children, ";
            treeSelect += "COALESCE(SUM(DISTINCT tree.size),0)+uf.size AS global_size, ";

            treeJoin = "LEFT JOIN (SELECT *  FROM uploaded_file t " +
                    "WHERE EXISTS (SELECT 1 FROM acl_sid AS asi LEFT JOIN acl_entry AS ae ON asi.id = ae.sid " +
                    "LEFT JOIN acl_object_identity AS aoi ON ae.acl_object_identity = aoi.id " +
                    "WHERE aoi.object_id_identity = t.storage_id AND asi.sid = :username) AND t.deleted IS NULL) " +
                    "AS tree ON (uf.l_tree @> tree.l_tree AND tree.id != uf.id) ";
        }

        String request = "SELECT uf.id, " +
                "uf.content_type, " +
                "uf.created, " +
                "uf.filename, " +
                "uf.original_filename, " +
                "uf.size, " +
                "uf.status, " +
                "uf.storage_id, " +
                "uf.user_id, " +
                "ai.height, " +
                "ai.magnification, " +
                "ai.width, " +
                "CASE WHEN (nlevel(uf.l_tree) > 0) THEN ltree2text(subltree(uf.l_tree, 0, 1)) ELSE NULL END AS root, " +
                treeSelect +
                "CASE WHEN (uf.status = " + UploadedFileStatus.CONVERTED.getCode() + " OR uf.status = " + UploadedFileStatus.DEPLOYED.getCode() + ") " +
                "THEN ai.id ELSE NULL END AS image " +
                "FROM uploaded_file uf " +
                treeJoin +
                "LEFT JOIN abstract_image AS ai ON ai.uploaded_file_id = uf.id " +
                "LEFT JOIN uploaded_file AS parent ON parent.id = uf.parent_id " +
                "WHERE EXISTS (SELECT 1 FROM acl_sid AS asi " +
                "LEFT JOIN acl_entry AS ae ON asi.id = ae.sid " +
                "LEFT JOIN acl_object_identity AS aoi ON ae.acl_object_identity = aoi.id " +
                "WHERE aoi.object_id_identity = uf.storage_id AND asi.sid = :username) " +
                "AND (uf.parent_id IS NULL OR parent.content_type IN ('" + String.join("','", UploadedFile.ARCHIVE_FORMATS) + "')) " +
                "AND uf.content_type NOT IN ('" + String.join("','", UploadedFile.ARCHIVE_FORMATS) + "') " +
                "AND uf.deleted IS NULL " +
                "AND " +
                (search.trim().isEmpty() ? "true" : search) +
                " GROUP BY uf.id, ai.id " +
                sort;


        Query query = getEntityManager().createNativeQuery(request, Tuple.class);
        Map<String, Object> mapParams = sqlSearchConditions.getSqlParameters();
        mapParams.put("username", currentUserService.getCurrentUsername());
        for (Map.Entry<String, Object> entry : mapParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        List<Tuple> resultList = query.getResultList();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Tuple rowResult : resultList) {
            Map<String, Object> result = new HashMap<>();
            for (TupleElement<?> element : rowResult.getElements()) {
                Object value = rowResult.get(element.getAlias());
                if (value instanceof BigInteger) {
                    value = ((BigInteger) value).longValue();
                }
                String alias = SQLUtils.toCamelCase(element.getAlias());
                result.put(alias, value);
            }
            result.put("thumbURL", (result.get("image") != null) ? UrlApi.getAbstractImageThumbUrl((Long) result.get("image"), "png") : null);
            result.put("isArchive", UploadedFile.ARCHIVE_FORMATS.contains(result.get("contentType")));
            results.add(result);
        }
        return results;

    }


    public List<Map<String, Object>> listHierarchicalTree(User user, Long rootId) {
        UploadedFile root = this.find(rootId)
                .orElseThrow(() -> new ObjectNotFoundException("UploadedFile", rootId));

        String request = "SELECT uf.id, uf.created, uf.original_filename, uf.content_type, " +
                "uf.l_tree, uf.parent_id as parent, " +
                "uf.size, uf.status, " +
                "cast (array_agg(ai.id) AS BIGINT[]) as image, cast (array_agg(asl.id) AS BIGINT[]) as slices, cast (array_agg(cf.id) AS BIGINT[]) as companion_file " +
                "FROM uploaded_file uf " +
                "LEFT JOIN abstract_image ai ON ai.uploaded_file_id = uf.id " +
                "LEFT JOIN abstract_slice asl ON asl.uploaded_file_id = uf.id " +
                "LEFT JOIN companion_file cf ON cf.uploaded_file_id = uf.id " +
                "LEFT JOIN acl_object_identity as aoi ON aoi.object_id_identity = uf.storage_id " +
                "LEFT JOIN acl_entry as ae ON ae.acl_object_identity = aoi.id " +
                "LEFT JOIN acl_sid as asi ON asi.id = ae.sid " +
                "WHERE uf.l_tree <@ CAST(CAST('" + root.getLTree() + "' as text) as ltree) " +
                "AND asi.sid = :username " +
                "AND uf.deleted IS NULL " +
                "GROUP BY uf.id , uf.l_tree " +
                "ORDER BY uf.l_tree ASC ";

        Query query = getEntityManager().createNativeQuery(request, Tuple.class);
        query.setParameter("username", user.getUsername());

        List<Tuple> resultList = query.getResultList();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Tuple rowResult : resultList) {
            Map<String, Object> result = new HashMap<>();
            for (TupleElement<?> element : rowResult.getElements()) {
                Object value = rowResult.get(element.getAlias());
                if (value instanceof BigInteger) {
                    value = ((BigInteger) value).longValue();
                }
                String alias = SQLUtils.toCamelCase(element.getAlias());
                result.put(alias, value);
            }

            result.put("image", (Arrays.stream((Long[]) rowResult.get("image"))).filter(x -> {
                if (x == null)
                    return false;
                Long z = (Long)x;
                boolean b = z != 0;
                return b;
            }).findFirst().orElse(null));
            result.put("slices", (Arrays.stream((Long[]) rowResult.get("slices"))).filter(x -> {
                if (x == null)
                    return false;
                Long z = (Long)x;
                boolean b = z!= 0;
                return b;
            }).collect(Collectors.toList())); // A same UF can be linked to several slices (virtual stacks)
            result.put("companionFile", (Arrays.stream((Long[]) rowResult.get("image"))).filter(x -> {
                if (x == null)
                    return false;
                Long z = (Long)x;
                boolean b = z != 0;
                return b;
            }).findFirst().orElse(null));
            result.put("thumbURL", null);

            // Hack: it seems that Hibernate-type return a Long if 1 element or a List<Long> if more than 1 element.
            Long image = returnElementOrTakeFirstElementIfArray(result.get("image"));
            Long slice = returnElementOrTakeFirstElementIfArray(result.get("slices"));
            if (image != null) {
                result.put("thumbURL", UrlApi.getAbstractImageThumbUrl(image, "png"));
                result.put("macroURL", UrlApi.getAssociatedImage(image, "abstractimage", "macro", (String) result.get("contentType"), 256, "png"));
            } else if (slice != null) {
                result.put("thumbURL", UrlApi.getAbstractSliceThumbUrl(slice, "png"));
            }

            result.put("isArchive", UploadedFile.ARCHIVE_FORMATS.contains(result.get("contentType")));
            results.add(result);
        }
        return results;

    }

    private Long returnElementOrTakeFirstElementIfArray(Object longOrLongArray) {
        if (longOrLongArray == null || (longOrLongArray instanceof List && ((List) longOrLongArray).isEmpty())) {
            return null;
        }
        return (longOrLongArray instanceof Long ? (Long) longOrLongArray : (Long) ((List) longOrLongArray).get(0));
    }

    public Optional<UploadedFile> find(Long id) {
        Optional<UploadedFile> uploadedFile = uploadedFileRepository.findById(id);
        uploadedFile.ifPresent(file -> securityACLService.check(file.container(), READ));
        return uploadedFile;
    }

    public UploadedFile get(Long id) {
        return find(id).orElse(null);
    }

    /**
     * Add the new domain with JSON data
     *
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        if (!json.isMissing("storage")) {
            securityACLService.check(json.getJSONAttrLong("storage"), Storage.class, WRITE);
        }
        return executeCommand(new AddCommand(currentUser), null, json);

    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        UploadedFile file = (UploadedFile) domain;
        file.updateLtree();
        uploadedFileRepository.save(file);
    }

    /**
     * Update this domain with new data from json
     *
     * @param domain      Domain to update
     * @param jsonNewData New domain datas
     * @return Response structure (new domain data, old domain data..)
     */
    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), WRITE);
        if (jsonNewData.get("storage") != null && !Objects.equals(jsonNewData.getJSONAttrLong("storage"), ((UploadedFile) domain).getStorage().getId())) {
            securityACLService.check(jsonNewData.getJSONAttrLong("storage"), Storage.class, WRITE);
        }
        return executeCommand(new EditCommand(currentUser, transaction), domain, jsonNewData);
    }

    /**
     * Delete an uploaded file root. As a consequence, all children are also deleted.
     *
     * @param domain       Domain to delete
     * @param transaction  Transaction link with this command
     * @param task         Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), WRITE);

        if (((UploadedFile) domain).getParent() != null) {
            throw new ForbiddenException("Uploaded File has a parent and can not be deleted.");
        }

        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c, domain, null);
    }

    /**
     * Delete an uploaded file child.
     */
    private CommandResponse deleteChild(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        /* We need another method than `delete()` because
           * uploaded file children have reference to uploaded file parent, thus they are no root.
           * `delete()` signature is standardized for its usage in RestCytomineController,
              so it is not possible to add a flag in the method.
         */
        User currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), WRITE);

        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c, domain, null);
    }


    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        UploadedFile uploadedFile = (UploadedFile) domain;
        return Arrays.asList(String.valueOf(uploadedFile.getId()), uploadedFile.getFilename());
    }


    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentAbstractSlice(domain, transaction, task);
        deleteDependentAbstractImage(domain, transaction, task);
        deleteDependentCompanionFile(domain, transaction, task);
        deleteDependentUploadedFile(domain, transaction, task);
    }

    private void deleteDependentAbstractImage(CytomineDomain domain, Transaction transaction, Task task) {
        log.info("deleteDependentAbstractImage");

        for (AbstractImage abstractImage : abstractImageRepository.findAllByUploadedFile((UploadedFile) domain)) {
            abstractImageService.delete(abstractImage, transaction, task, false);
        }
    }

    private void deleteDependentAbstractSlice(CytomineDomain domain, Transaction transaction, Task task) {
        log.info("deleteDependentAbstractSlice");

        for (AbstractSlice abstractSlice : abstractSliceRepository.findAllByUploadedFile((UploadedFile) domain)) {
            abstractSliceService.delete(abstractSlice, transaction, task, false);
        }
    }

    private void deleteDependentCompanionFile(CytomineDomain domain, Transaction transaction, Task task) {
        log.info("deleteDependentCompanionFile");

        for (CompanionFile companionFile : companionFileRepository.findAllByUploadedFile((UploadedFile) domain)) {
            companionFileService.delete(companionFile, transaction, task, false);
        }
    }

    private void deleteDependentUploadedFile(CytomineDomain domain, Transaction transaction, Task task) {
        log.info("deleteDependentUploadedFile");
        UploadedFile uploadedFile = (UploadedFile) domain;
        taskService.updateTask(task, task != null ? "Delete " + uploadedFileRepository.countByParent(uploadedFile) + " uploadedFiles children" : "");

        // Dependent uploaded files are necessarily children, because only a root can be deleted.
        for (UploadedFile child : uploadedFileRepository.findAllByParent(uploadedFile)) {
            this.deleteChild(child, transaction, task, false);
        }
    }


}
