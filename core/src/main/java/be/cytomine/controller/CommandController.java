package be.cytomine.controller;

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

import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.RedoStackItem;
import be.cytomine.domain.command.UndoStackItem;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.command.CommandRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("")
@Slf4j
@RequiredArgsConstructor
public class CommandController extends RestCytomineController {

    private final CurrentUserService currentUserService;

    private final CommandRepository commandRepository;

    private final MessageSource messageSource;

    private final CommandService commandService;


    @GetMapping({"/command/undo.json", "/api/command/undo.json","/command/{id}/undo.json", "/api/command/{id}/undo.json"})
    public ResponseEntity<String> undo(
            @PathVariable(required = false) Long id
    ) {
        log.debug("REST request to undo command {}", id);
        User user = currentUserService.getCurrentUser();
        Command command = null;
        if (id!=null) {
            command = commandRepository.findById(id)
                    .orElseThrow(() -> new ObjectNotFoundException("Command" , id));
        }


        //Get the last command list with max 1 command

        Optional<UndoStackItem> lastCommand;
        if (command!=null) {
            lastCommand = commandRepository.findLastUndoStackItem(user, command);
        } else {
            lastCommand = commandRepository.findLastUndoStackItem(user);
        }

        //There is no command, so nothing to undo
        if (lastCommand.isEmpty()) {
            String message = messageSource.getMessage("be.cytomine.UndoCommand", new Object[0], Locale.ENGLISH);
            return responseSuccess(List.of(JsonObject.of("success", true, "message", message, "callback", null, "printMessage", true)));
        }

        //Last command done
        UndoStackItem firstUndoStack = lastCommand.get();
        List<CommandResponse> results = commandService.undo(firstUndoStack, user);


        if(results.stream().anyMatch(x -> x.getStatus()!=200 && x.getStatus()!=201)) {
            response.setStatus(400);
        } else {
            response.setStatus(200);
        }
        return responseSuccess(results.stream().map(x -> x.getData()).toList());
    }


    @GetMapping({"/command/redo.json", "/command/{id}/redo.json", "/api/command/redo.json", "/api/command/{id}/redo.json"})
    public ResponseEntity<String> redo(
            @PathVariable(required = false) Long id
    ) {
        log.debug("REST request to undo command {}", id);
        User user = currentUserService.getCurrentUser();
        Command command = null;
        if (id!=null) {
            command = commandRepository.findById(id)
                    .orElseThrow(() -> new ObjectNotFoundException("Command" , id));
        }


        //Get the last command list with max 1 command

        Optional<RedoStackItem> lastCommand;
        if (command!=null) {
            lastCommand = commandRepository.findLastRedoStackItem(user, command);
        } else {
            lastCommand = commandRepository.findLastRedoStackItem(user);
        }

        //There is no command, so nothing to undo
        if (lastCommand.isEmpty()) {
            String message = messageSource.getMessage("be.cytomine.RedoCommand", new Object[0], Locale.ENGLISH);
            return responseSuccess(List.of(JsonObject.of("success", true, "message", message, "callback", null, "printMessage", true)));
        }

        //Last command done
        RedoStackItem firstRedoStack = lastCommand.get();
        List<CommandResponse> results = commandService.redo(firstRedoStack, user);


        if(results.stream().anyMatch(x -> x.getStatus()!=200 && x.getStatus()!=201)) {
            response.setStatus(400);
        } else {
            response.setStatus(200);
        }
        return responseSuccess(results.stream().map(x -> x.getData()).toList());
    }


    @GetMapping({"/api/deletecommand.json", "/api/deletecommand"}) // without json for backward compatibility
    public ResponseEntity<String> listDelete(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false, value = "after") Long afterThan) {
        return responseSuccess(commandService.list(domain, DeleteCommand.class, afterThan));
    }

}
