package be.cytomine.utils;

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

import be.cytomine.service.utils.TaskService;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * A task provide info about a command.
 * The main info is the progress status
 * THIS CLASS CANNOT BE A DOMAIN! Because it cannot works with hibernate transaction.
 */
@Getter
@Setter
public class Task {

    private Long id;

    /**
     * Request progress between 0 and 100
     */
    private int progress = 0;

    /**
     * Project updated by the command task
     */
    private Long projectIdent = -1L;

    /**
     * User that ask the task
     */
    private Long userIdent;

    private boolean printInActivity = false;


    public JsonObject toJsonObject() {
        JsonObject map = new JsonObject();
        map.put("id", id);
        map.put("progress", progress);
        map.put("project", projectIdent);
        map.put("user", userIdent);
        map.put("printInActivity", printInActivity);
        return map;
    }

}
