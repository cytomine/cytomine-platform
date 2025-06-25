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

import be.cytomine.domain.command.*;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.CytomineException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.command.CommandRepository;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class CommandService {
    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    EntityManager entityManager;

    @Autowired
    CommandRepository commandRepository;

    @Autowired
    CurrentUserService currentUserService;

    @Autowired
    ResponseService responseService;

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    SecurityACLService securityACLService;

    static final int SUCCESS_ADD_CODE = 200;
    static final int SUCCESS_EDIT_CODE = 200;
    static final int SUCCESS_DELETE_CODE = 200;

    CommandResponse processCommand(Command c, ModelService service) throws CytomineException {
        if (c instanceof AddCommand) {
            return processCommand(c, service, SUCCESS_ADD_CODE);
        }
        if (c instanceof EditCommand) {
            return processCommand(c, service, SUCCESS_EDIT_CODE);
        }
        if (c instanceof DeleteCommand) {
            return processCommand(c, service, SUCCESS_DELETE_CODE);
        }
        throw new ObjectNotFoundException("Command not supported");
    }

    /**
     * Execute a 'command' c with json data
     * Store command in undo stack if necessary and in command history
     * if success, put http response code as successCode
     */
    CommandResponse processCommand(Command c, ModelService service, int successCode) throws CytomineException {
        //execute command
        log.debug("processCommand");
        CommandResponse result = c.execute(service);
        if (result.getStatus() == successCode) {
            if ((service instanceof ProjectService && c instanceof DeleteCommand)) {
                c.setProject(null); // project has been deleted in this command, so we cannot link the command to the deleted project
            }

            entityManager.persist(c);

            CommandHistory ch = new CommandHistory(c);
            entityManager.persist(ch);

            if (c.isSaveOnUndoRedoStack()) {
                UndoStackItem item = new UndoStackItem();
                item.setCommand(c);
                item.setUser(c.getUser());
                item.setTransaction(c.getTransaction());
                entityManager.persist(item);
                //entityManager.flush();
            }
            result.getData().put("command", c.getId());
        }
        return result;
    }

    public List<CommandResponse> undo() {
        return undo(null);
    }

    public List<CommandResponse> undo(Long commandId) {
        User user = currentUserService.getCurrentUser();
        Optional<UndoStackItem> lastUndoStackItem;
        if (commandId!=null) {
            lastUndoStackItem = commandRepository.findLastUndoStackItem(user, commandRepository.getById(commandId));
        } else {
            lastUndoStackItem = commandRepository.findLastUndoStackItem(user);
        }

        if (lastUndoStackItem.isEmpty()) {
            //There is no command, so nothing to undo
            CommandResponse commandResponse = new CommandResponse();
            commandResponse.setData(responseService.createResponseData(true, "be.cytomine.UndoCommand", null, true));
            commandResponse.setStatus(200);
            return List.of(commandResponse);
        } else {
            UndoStackItem firstUndoStack = lastUndoStackItem.get();
            return this.undo(firstUndoStack, user);
        }
    }

    private ModelService loadCorrespondingModelService(Command command) {
        if (command.getServiceName()==null || command.getServiceName().length()<1 ) {
            throw new RuntimeException("Bean definition name " + command.getServiceName() + " is null or blank");
        }
        return (ModelService)beanFactory.getBean(Character.toLowerCase(command.getServiceName().charAt(0)) + command.getServiceName().substring(1));
    }

    private CommandResponse performUndo(Command command) {
        ModelService modelService = loadCorrespondingModelService(command);
        if (command instanceof AddCommand) {
            return modelService.destroy(JsonObject.toJsonObject(command.getData()), command.isPrintMessage());
        } else if (command instanceof EditCommand) {
            return modelService.edit(JsonObject.toJsonObject(command.getData()).extractProperty("previous" + ((EditCommand) command).domainName()), command.isPrintMessage());
        } else if (command instanceof DeleteCommand) {
            return modelService.create(JsonObject.toJsonObject(command.getData()), command.isPrintMessage());
        }
        throw new RuntimeException("not yet implemented");
    }

    public List<CommandResponse> undo(UndoStackItem undoItem, User user) {
        CommandResponse result;
        List<CommandResponse> results = new ArrayList<>();

        Transaction transaction =  undoItem.getTransaction();
        log.debug("FirstUndoStack=" + undoItem);

        if (transaction==null) {
            log.debug("Transaction not in progress");
            //Not Transaction, no other command must be deleted
            result = performUndo(undoItem.getCommand());
            //An undo command must be move to redo stack
            moveToRedoStack(undoItem);
            results.add(result);
        } else {
            log.debug("Transaction in progress");
            //Its a transaction, many other command will be deleted
            List<UndoStackItem> undoStacks = commandRepository.findAllUndoOrderByCreatedDesc(user, transaction);
            for (UndoStackItem undoStack : undoStacks) {
                //browse all command and undo it while its the same transaction
                if(undoStack.getCommand().isRefuseUndo()) {
                    //responseError(new ObjectNotFoundException("You cannot delete your last operation!")) //undo delete project is not possible
                    throw new ObjectNotFoundException("You cannot delete your last operation!"); //undo delete project is not possible
                }
                result = performUndo(undoStack.getCommand());
                log.info("Undo stack transaction: " + result);
                results.add(result);
                moveToRedoStack(undoStack);
            }
        }

        return results;
    }

    public List<CommandResponse> redo() {
        return redo(null);
    }

    public List<CommandResponse> redo(Long commandId) {
        User user = currentUserService.getCurrentUser();
        Optional<RedoStackItem> lastRedoStackItem;
        if (commandId!=null) {
            lastRedoStackItem = commandRepository.findLastRedoStackItem(user, commandRepository.getById(commandId));
        } else {
            lastRedoStackItem = commandRepository.findLastRedoStackItem(user);
        }

        if (lastRedoStackItem.isEmpty()) {
            //There is no command, so nothing to undo
            CommandResponse commandResponse = new CommandResponse();
            commandResponse.setData(responseService.createResponseData(true, "be.cytomine.RedoCommand", null, true));
            commandResponse.setStatus(200);
            return List.of(commandResponse);
        } else {
            RedoStackItem redoStackItem = lastRedoStackItem.get();
            return this.redo(redoStackItem, user);
        }
    }

    private CommandResponse performRedo(Command command) {
        ModelService modelService = loadCorrespondingModelService(command);
        if (command instanceof AddCommand) {
            return modelService.create(JsonObject.toJsonObject(command.getData()), command.isPrintMessage());
        } else if (command instanceof EditCommand) {
            return modelService.edit(JsonObject.toJsonObject(command.getData()).extractProperty("new" + ((EditCommand) command).domainName()), command.isPrintMessage());
        } else if (command instanceof DeleteCommand) {
            return modelService.destroy(JsonObject.toJsonObject(command.getData()), command.isPrintMessage());
        }
        throw new RuntimeException("not yet implemented");
    }

    public List<CommandResponse> redo(RedoStackItem redoItem, User user) {
        CommandResponse result;
        List<CommandResponse> results = new ArrayList<>();

        Transaction transaction =  redoItem.getTransaction();
        log.debug("LastRedoStack=" + redoItem);

        if (transaction==null) {
            log.debug("Transaction not in progress");
            //Not Transaction, no other command must be deleted
            result = performRedo(redoItem.getCommand());
            //An undo command must be move to redo stack
            moveToUndoStack(redoItem);
            results.add(result);
        } else {
            log.debug("Transaction in progress");
            //Its a transaction, many other command will be deleted
            List<RedoStackItem> redoStacks = commandRepository.findAllRedoOrderByCreatedDesc(user, transaction);
            for (RedoStackItem redoStack : redoStacks) {
                //Redo each command from the same transaction
                result = performRedo(redoStack.getCommand());
                moveToUndoStack(redoStack);
                results.add(result);
            }
        }
        return results;
    }

    /**
     * Move an undo stack item to redo stack
     * @param firstUndoStack Undo stack item to move
     */
    private void moveToRedoStack(UndoStackItem firstUndoStack) {
        //create new redo stack item
        RedoStackItem redoStackItem = new RedoStackItem(firstUndoStack);
        entityManager.persist(redoStackItem);

        CommandHistory commandHistory = new CommandHistory(firstUndoStack);
        entityManager.persist(commandHistory);

        if (entityManager.contains(firstUndoStack)) {
            entityManager.remove(firstUndoStack);
        }
        entityManager.flush();
    }

    /**
     * Move redo item to the undo stack
     * @param lastRedoStack redo item to move
     */
    private void moveToUndoStack(RedoStackItem lastRedoStack) {
        //create the new undo item
        UndoStackItem undoStackItem = new UndoStackItem(lastRedoStack);
        entityManager.persist(undoStackItem);
        //add to history stack
        CommandHistory commandHistory = new CommandHistory(lastRedoStack);
        entityManager.persist(commandHistory);
        //delete the redo item
        if (entityManager.contains(lastRedoStack)) {
            entityManager.remove(lastRedoStack);
        }
        entityManager.flush();
    }

    public List<Command> list(String domain, Class commandClass, Long afterThan) {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        List<Command> commands = new ArrayList<>();
        if(domain!=null) {
            String serviceName = domain.substring(0, 1).toUpperCase() + domain.substring(1) + "Service";

            if(afterThan!=null){
                commands = commandRepository.findAllByServiceNameAndCreatedGreaterThan(serviceName, new Date(afterThan));
            } else {
                commands = commandRepository.findAllByServiceName(serviceName);
            }
        } else {
            commands =  commandRepository.findAll();
        }
        return commands.stream().filter(x -> x.getClass().equals(commandClass)).toList();
    }
}
