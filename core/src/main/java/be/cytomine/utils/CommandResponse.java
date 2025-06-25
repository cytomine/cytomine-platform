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

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.service.UrlApi;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CommandResponse {

    Integer status;

    CytomineDomain object;

    Map<String, Object> data;

    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public static JsonObject getDataFromDomain(CommandResponse domain) {
       JsonObject jsonObject = new JsonObject();
       jsonObject.put("status", domain.getStatus());
       jsonObject.put("object", domain.getObject().toJsonObject());
       jsonObject.put("data", domain.getData());
       return jsonObject;
    }

}
