package be.cytomine.domain;

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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * When a domain has not a real reference to its container (e.g. algoAnnotationTerm.annotationIdent + class ; description.domainIdent + class), we fill this object with id/class.
 * When we perform an ACL, there is a special case to load the object from the database before calling its .container().
 * This is a hack because we cannot load the object directly from the DOMAIN.container() method
 */
public class GenericCytomineDomainContainer extends CytomineDomain {

    private String containerClass;

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    @Override
    public String toString() {
        return "GenericCytomineDomainContainer{" +
                "id=" + id +
                ", containerClass='" + containerClass + '\'' +
                '}';
    }

    public CytomineDomain container() {
        return this;
    }


}
