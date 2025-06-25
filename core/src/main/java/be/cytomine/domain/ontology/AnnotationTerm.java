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
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Getter
@Setter
public class AnnotationTerm extends CytomineDomain implements Serializable {


    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_annotation_id", nullable = false)
    private UserAnnotation userAnnotation;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AnnotationTerm relationTerm = this;
        relationTerm.id = json.getJSONAttrLong("id",null);
        relationTerm.userAnnotation = (UserAnnotation)json.getJSONAttrDomain(entityManager, "userannotation", new UserAnnotation(), true);
        relationTerm.term = (Term)json.getJSONAttrDomain(entityManager, "term", new Term(), true);
        relationTerm.user = (User)json.getJSONAttrDomain(entityManager, "user", new User(), true);
        relationTerm.created = json.getJSONAttrDate("created");
        relationTerm.updated = json.getJSONAttrDate("updated");

        if(relationTerm.term.getOntology()!=relationTerm.userAnnotation.project.getOntology()) {
            throw new WrongArgumentException("Term " +term.getName()+ " from ontology " + term.getOntology().getName() + " is not in ontology from the annotation project");
        }
        return relationTerm;
    }

    public User userDomainCreator() {
        return user;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AnnotationTerm annotationTerm = (AnnotationTerm)domain;
        returnArray.put("userannotation", (annotationTerm.getUserAnnotation()!=null ? annotationTerm.getUserAnnotation().getId() : null));
        returnArray.put("term", (annotationTerm.getTerm()!=null ? annotationTerm.getTerm().getId() : null));
        returnArray.put("user", (annotationTerm.getUser()!=null ? annotationTerm.getUser().getId() : null));
        return returnArray;
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
        return userAnnotation.container();
    }

}
