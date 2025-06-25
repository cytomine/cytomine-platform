package be.cytomine.repository.ontology;

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

import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.RelationTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RelationRepository extends JpaRepository<Relation, Long>, JpaSpecificationExecutor<Relation>  {

    Relation getByName(String name);

    default Relation getParent() {
        return getByName(RelationTerm.PARENT);
    }

    default Relation createIfNotExist(String name) {
        Relation relation = getByName(name);
        if (relation==null) {
            relation = new Relation();
            relation.setName(name);
            relation = save(relation);
        }
        return relation;
    }
}
