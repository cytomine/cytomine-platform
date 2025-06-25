package be.cytomine.service;

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
import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.exceptions.*;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.meta.AttachedFileService;
import be.cytomine.service.meta.DescriptionService;
import be.cytomine.service.meta.PropertyService;
import be.cytomine.service.meta.TagDomainAssociationService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Transactional
@Slf4j
public abstract class ModelService<T extends CytomineDomain> {

    @Autowired
    EntityManager entityManager;

    @Autowired
    ResponseService responseService;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    CommandService commandService;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    UserAnnotationRepository userAnnotationRepository;

    @Autowired
    PropertyService propertyService;

    @Autowired
    DescriptionService descriptionService;

    @Autowired
    AttachedFileService attachedFileService;

    @Autowired
    TagDomainAssociationService tagDomainAssociationService;

    public Long generateNextId() {
        return sequenceService.generateID();
    }

    /**
     * Save a domain on database, throw error if cannot save
     */
    public void saveDomain(CytomineDomain newObject) {
        checkDoNotAlreadyExist(newObject);

        try {
            if (newObject.getId()!=null && !entityManager.contains(newObject)) {
                log.debug("object detached");
                // entity is detached, merge it in the session
                newObject = entityManager.merge(newObject);
            }
            entityManager.persist(newObject);
            entityManager.flush();
        } catch (OptimisticLockingFailureException e) {
            log.error("CANNOT SAVE OBJECT");
            newObject = entityManager.merge(newObject);
        } catch(Exception e) {
            throw new WrongArgumentException("Cannot persists object:" + e);
        }
    }

    /**
     * Delete a domain from database
     */
    public void removeDomain(CytomineDomain oldObject) {
        try {
            entityManager.refresh(oldObject);
            entityManager.remove(oldObject);
            entityManager.flush();
        } catch (Exception e) {
            log.error(e.toString());
            throw new InvalidRequestException(e.toString());
        }
    }

    /**
     * Add command info for the new domain concerned by the command
     */
    CytomineDomain fillDomainWithData(CytomineDomain object, JsonObject json) {
        CytomineDomain domain = entityManager.find(object.getClass(), retrieveLongId(json));
        domain.buildDomainFromJson(json, this.entityManager);
        domain.setId(json.getJSONAttrLong("id"));
        return domain;
    }

    /**
     * Get the name of the service (project,...)
     */
    public String getServiceName() {
        return this.getClass().getSimpleName();
    }

    public CommandResponse executeCommand(Command c, CytomineDomain domain, JsonObject json) {
        return executeCommand(c, domain, json, null);
    }

    public CommandResponse executeCommand(Command c, CytomineDomain domain, JsonObject json, Task task) {
        //bug, cannot do new XXXCommand(domain:domain, json:...) => new XXXCommand(); c.domain = domain; c.json = ...
        c.setDomain(domain);
        c.setJson(json);
        return executeCommand(c,task);
    }

    public CommandResponse executeCommand(Command c) {
        return executeCommand(c, null);
    }
    /**
     * Execute command with JSON data
     */
    public CommandResponse executeCommand(Command c, Task task) {
        log.debug("Command " + c.getClass() + " " + getServiceName());
        if(c instanceof DeleteCommand) {
            CytomineDomain domainToDelete = c.getDomain();
            //Create a backup (for 'undo' op)
            //We create before for deleteCommand to keep data from HasMany inside json (data will be deleted later)
            Object backup = domainToDelete.toJSON();
            ((DeleteCommand) c).setBackup(backup);
            this.deleteDependencies(domainToDelete, c.getTransaction(), task);

        }
        initCommandService();
        c.setSaveOnUndoRedoStack(this.isSaveOnUndoRedoStack()); //need to use getter method, to get child value
        c.setServiceName(getServiceName());
        return commandService.processCommand(c, this);
    }

    private void initCommandService() {
        if (commandService == null) {
            commandService = (CommandService)applicationContext.getBean("commandService");
        }

    }

    public Boolean isSaveOnUndoRedoStack() {
        return true;
    }

    public CommandResponse add(JsonObject jsonObject) {
        throw new CytomineMethodNotYetImplementedException("No add method implemented");
    }

    public CommandResponse add(JsonObject jsonObject, Task task) {
        throw new CytomineMethodNotYetImplementedException("No add method implemented with task");
    }

    public CommandResponse add(JsonObject jsonObject, Transaction transaction, Task task) {
        throw new CytomineMethodNotYetImplementedException("No add method implemented with transaction/task");
    }

    public abstract Class currentDomain();

    /**
     * Create domain from JSON object
     * @param json JSON with new domain info
     * @return new domain
     */
    public abstract CytomineDomain createFromJSON(JsonObject json);

    /**
     * Create new domain in database
     * @param json JSON data for the new domain
     * @param printMessage Flag to specify if confirmation message must be show in client
     * Usefull when we create a lot of data, just print the root command message
     * @return Response structure (status, object data,...)
     */
    CommandResponse create(JsonObject json, boolean printMessage) {
        return create(createFromJSON(json), printMessage);
    }

    /**
     * Create new domain in database
     * @param domain Domain to store
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    public CommandResponse create(CytomineDomain domain, boolean printMessage) {
        //Save new object
        beforeAdd(domain);
        saveDomain(domain);

        CommandResponse response = responseService.createResponseMessage(domain, getStringParamsI18n(domain), printMessage, "Add", domain.getCallBack());
        afterAdd(domain,response);
        //Build response message
        return response;
    }


    /**
     * Edit domain from database
     * @param json domain data in json
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    public CommandResponse edit(JsonObject json, boolean printMessage) {
        //Rebuilt previous state of object that was previoulsy edited
        try {
            return edit(fillDomainWithData(((CytomineDomain)currentDomain().getDeclaredConstructor().newInstance()), json), printMessage);
        } catch (Exception e) {
            throw new ObjectNotFoundException("Cannot create instance of object: " + json + " Exception " + e);
        }
    }

    /**
     * Edit domain from database
     * @param domain Domain to update
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    public CommandResponse edit(CytomineDomain domain, boolean printMessage) {
        //Build response message
        beforeUpdate(domain);
        saveDomain(domain);
        CommandResponse response = responseService.createResponseMessage(domain, getStringParamsI18n(domain), printMessage, "Edit", domain.getCallBack());
        afterUpdate(domain, response);
        return response;
    }


    /**
     * Destroy domain from database
     * @param json JSON with domain data (to retrieve it)
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    public CommandResponse destroy(JsonObject json, boolean printMessage) {
        //Get object to delete
        return destroy((CytomineDomain)entityManager.find(currentDomain(), retrieveLongId(json)), printMessage);
    }

    /**
     * Destroy domain from database
     * @param domain Domain to remove
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    public CommandResponse destroy(CytomineDomain domain, boolean printMessage) {
        //Build response message
        CommandResponse response = responseService.createResponseMessage(domain, getStringParamsI18n(domain), printMessage, "Delete", domain.getCallBack());
        beforeDelete(domain);
        //Delete object
        removeDomain(domain);
        afterDelete(domain,response);
        return response;
    }

    /**
     * Retrieve domain thanks to a JSON object
     * @return domain retrieve thanks to json
     */
    public CytomineDomain retrieve(JsonObject json) {

        CytomineDomain domain = null;
        if(json.containsKey("id") && !json.get("id").toString().equals("null")) {
            domain = (CytomineDomain) entityManager.find(currentDomain(), retrieveLongId(json)); // cast to long = issue ? TODO
        }

        if (domain == null) {
            throw new ObjectNotFoundException(currentDomain() + " " + json.get("id") + " not found");
        }
        CytomineDomain container = domain.container();
        if (container!=null) {
            //we only check security if container is defined
            securityACLService.check(container,READ);
        }
        return domain;
    }

    protected Long retrieveLongId(JsonObject json) {
        Long id;
        if (json.get("id") instanceof String) {
            id = Long.parseLong((String) json.get("id"));
        } else if (json.get("id") instanceof Integer) {
            id = ((Integer) json.get("id")).longValue();
        }  else {
            id = (Long) json.get("id");
        }
        return id;
    }

    protected void beforeAdd(CytomineDomain domain) {
    }

    protected void beforeDelete(CytomineDomain domain) {

    }

    protected void beforeUpdate(CytomineDomain domain) {

    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
    }

    protected void afterDelete(CytomineDomain domain, CommandResponse response) {

    }

    protected void afterUpdate(CytomineDomain domain, CommandResponse response) {

    }

    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        throw new ServerException("getStringParamsI18n must be implemented for " + this.getClass().toString());
    }

    /**
     * Build callback data for a domain (by default null)
     * Callback are metadata used by client
     * You need to override getCallBack() in domain class
     * @return Callback data
     */
    public Map<String, Object> getCallBack() {
        return null;
    }

    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData) {
        return update(domain, jsonNewData, null);
    }


    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        throw new CytomineMethodNotYetImplementedException("No update method implemented");
    }

    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction, Task task) {
        if (task==null) {
            return update(domain, jsonNewData, transaction);
        } else {
            throw new CytomineMethodNotYetImplementedException("No update method implemented with task");
        }
    }

    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        throw new CytomineMethodNotYetImplementedException("No delete method implemented");
    }

    protected void deleteDependentMetadata(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentProperty(domain, transaction, task);
        deleteDependentDescription(domain, transaction, task);
        deleteDependentAttachedFile(domain, transaction, task);
        deleteDependentTagDomainAssociation(domain, transaction, task);
    }
    protected void deleteDependentProperty(CytomineDomain domain, Transaction transaction, Task task) {
        for (Property property : propertyService.list(domain)) {
            propertyService.delete(property, transaction, task, false);
        }
    }
    protected void deleteDependentDescription(CytomineDomain domain, Transaction transaction, Task task) {
        descriptionService.findByDomain(domain).ifPresent(description -> {
            descriptionService.delete(description, transaction, task, false);
        });
    }

    protected void deleteDependentAttachedFile(CytomineDomain domain, Transaction transaction, Task task) {
        for (AttachedFile attachedFile : attachedFileService.findAllByDomain(domain)) {
            attachedFileService.delete(attachedFile, transaction, task, false);
        }
    }

    protected void deleteDependentTagDomainAssociation(CytomineDomain domain, Transaction transaction, Task task) {
        for (TagDomainAssociation tagDomainAssociation : tagDomainAssociationService.listAllByDomain(domain)) {
            tagDomainAssociationService.delete(tagDomainAssociation, transaction, task, false);
        }
    }


    public void checkDoNotAlreadyExist(CytomineDomain domain) {
        // do nothing by default
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        return;
    }

    public CytomineDomain getCytomineDomain(String domainClassName, Long domainIdent) {
        try {
            return (CytomineDomain)getEntityManager()
                    .find(Class.forName(domainClassName), domainIdent);
        } catch (ClassNotFoundException e) {
            throw new ObjectNotFoundException(domainClassName, domainIdent);
        }
    }


    public CommandResponse addOne(JsonObject json){
        return add(json);
    }

    public JsonObject addMultiple(List<JsonObject> json) {
        List<JsonObject> result = new ArrayList();
        List errors = new ArrayList();
        JsonObject resp;
        for(int i=0;i<json.size();i++){
            try{
                CommandResponse commandResponse = addOne(json.get(i));

                String objectName;
                if (currentDomain() == UserAnnotation.class) {
                    objectName = "annotation";
                } else {
                    String[] split = currentDomain().toString().toLowerCase().split("\\.");
                    objectName = split[split.length-1];
                }
                resp = JsonObject.of("domain", ((Map<String, Object>)commandResponse.getData().get(objectName)).get("id"), "status", commandResponse.getStatus());
            } catch(CytomineException e){
                log.info(((CytomineException)e).getMessage());
                errors.add(JsonObject.of("data", json.get(i), "message", e.msg));
                resp = JsonObject.of("message", e.msg, "status", e.code);
            } catch(Exception e) {
                log.info(e.toString());
                resp = JsonObject.of("message", e.toString(), "status", 500);
            }

            result.add(resp);
        }

        JsonObject response = new JsonObject();

        List<JsonObject> succeeded = result.stream().filter(x -> x.getJSONAttrInteger("status")>=200 && x.getJSONAttrInteger("status")<=300).toList();

        if(succeeded.size() == result.size()) {
            String[] split = currentDomain().toString().toLowerCase().split("\\.");
            response.put("data", JsonObject.of("message", split[split.length-1]+"s "+succeeded.stream().map(x -> x.getJSONAttrStr("domain")).collect(Collectors.joining(","))+" added"));
            response.put("status", 200);
        } else if(succeeded.size() == 0) {
            response.put("data", JsonObject.of("success", false, "message", "No entry saved", "errors", errors));
            response.put("status", 400);
        } else {
            String[] split = currentDomain().toString().toLowerCase().split("\\.");
            response.put("data", JsonObject.of("success", false, "message", "Only part of the entries (" + split[split.length-1]+"s " + succeeded.stream().map(x -> x.getJSONAttrStr("domain")).collect(Collectors.joining(",")), "errors", errors));
            response.put("status", 206);
        }
        return response;
    }

}
