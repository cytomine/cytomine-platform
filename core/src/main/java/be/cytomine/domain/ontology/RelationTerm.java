package be.cytomine.domain.ontology;

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
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Getter
@Setter
public class RelationTerm extends CytomineDomain implements Serializable {

    public static String PARENT = "parent";
    public static String SYNONYM = "synonyme";

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "relation_id", nullable = false)
    private Relation relation;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "term1_id", nullable = false)
    private Term term1;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "term2_id", nullable = false)
    private Term term2;



    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        RelationTerm relationTerm = this;
        relationTerm.id = json.getJSONAttrLong("id",null);
        relationTerm.relation = (Relation)json.getJSONAttrDomain(entityManager, "relation", new Relation(), true);
        relationTerm.term1 = (Term)json.getJSONAttrDomain(entityManager, "term1", new Term(), true);
        relationTerm.term2 = (Term)json.getJSONAttrDomain(entityManager, "term2", new Term(), true);
        relationTerm.created = json.getJSONAttrDate("created");
        relationTerm.updated = json.getJSONAttrDate("updated");
        return relationTerm;
    }



    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        RelationTerm relationTerm = (RelationTerm)domain;
        returnArray.put("relation", (relationTerm!=null ? relationTerm.getRelationId() : null));
        returnArray.put("term1", (relationTerm!=null ?relationTerm.getTerm1Id() : null));
        returnArray.put("term2", (relationTerm!=null ?relationTerm.getTerm2Id() : null));
        return returnArray;
    }

    private Long getRelationId() {
        return (relation!=null? relation.getId() : null);
    }

    private Long getTerm1Id() {
        return (term1!=null? term1.getId() : null);
    }

    private Long getTerm2Id() {
        return (term2!=null? term2.getId() : null);
    }


    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public CytomineDomain container() {
        return term1.container();
    }

    @Override
    public String toString() {
        return "RelationTerm{" +
                "relation=" + relation +
                ", term1=" + term1 +
                ", term2=" + term2 +
                '}';
    }
}
