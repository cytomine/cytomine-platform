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

import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class TaskController extends RestCytomineController {

    private final CurrentUserService currentUserService;

    private final ProjectService projectService;

    private final TaskService taskService;

    @GetMapping("/task/{id}.json")
    public ResponseEntity<String> show(@PathVariable Long id) {
        Task task = taskService.get(id);
        if (task == null) {
            throw new ObjectNotFoundException("Task", id);
        }
        JsonObject jsonObject = task.toJsonObject();
        jsonObject.put("comments", taskService.getLastComments(task,5));
        return responseSuccess(jsonObject);
    }

    @PostMapping("/task.json")
    public ResponseEntity<String> add(@RequestBody JsonObject json) {
        Project project = null;
        try {
            project = projectService.get(json.getJSONAttrLong("project", 0L));
        } catch(Exception ignored) {

        }
        User user = currentUserService.getCurrentUser();
        boolean printInActivity = json.getJSONAttrBoolean("printInActivity", false);
        Task task = taskService.createNewTask(project,user,printInActivity);
        JsonObject jsonObject = task.toJsonObject();
        jsonObject.put("comments", taskService.getLastComments(task,5));
        return responseSuccess(JsonObject.of("task", jsonObject));
    }

    @GetMapping("/project/{project}/task/comment.json")
    public ResponseEntity<String> listCommentByProject(@PathVariable(value = "project") Long projectId) {
        Project project = projectService.find(projectId)
                .orElseThrow(() -> new ObjectNotFoundException("Project", projectId));
        return responseSuccess(taskService.listLastComments(project));
    }
}
