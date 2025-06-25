package be.cytomine.dto;

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

import be.cytomine.utils.JsonObject;
import be.cytomine.utils.MinMax;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
@Getter
public class ProjectBounds extends AbstractBounds {

    private MinMax<Date> created = new MinMax<>();

    private MinMax<Date> updated = new MinMax<>();

    private MinMax<String> mode = new MinMax<>();

    private MinMax<String> name = new MinMax<>();

    private MinMax<Long> numberOfAnnotations = new MinMax<>();

    private MinMax<Long> numberOfJobAnnotations = new MinMax<>();

    private MinMax<Long> numberOfReviewedAnnotations = new MinMax<>();

    private MinMax<Long> numberOfImages = new MinMax<>();

    private MinMax<Long> members = new MinMax<>();

    public void submit(JsonObject project) {
        log.debug(project.toJsonString());
        updateMinMax(created, project.getJSONAttrDate("created"));
        updateMinMax(updated, project.getJSONAttrDate("updated"));

        updateMinMax(mode, project.getJSONAttrStr("mode"));
        updateMinMax(name, project.getJSONAttrStr("name"));
        updateMinMax(numberOfAnnotations, project.getJSONAttrLong("numberOfAnnotations"));
        updateMinMax(numberOfJobAnnotations, project.getJSONAttrLong("numberOfJobAnnotations"));

        updateMinMax(numberOfReviewedAnnotations, project.getJSONAttrLong("numberOfReviewedAnnotations"));
        updateMinMax(numberOfImages, project.getJSONAttrLong("numberOfImages"));
        updateMinMax(members, project.getJSONAttrLong("membersCount"));
    }
}
