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
import be.cytomine.domain.GenericCytomineDomainContainer;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
/**
 * A shared annotation is a comment on a specific annotation
 * (e.g. is it the good term?, ...)
 * Receiver user can see the comment and add answer
 */
public class SharedAnnotation extends CytomineDomain implements Serializable {

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    private String comment;


    @NotNull
    private String annotationClassName;

    @NotNull
    @Column(name = "annotation_ident")
    private Long annotationIdent;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "shared_annotation_user",
            joinColumns = { @JoinColumn(name = "shared_annotation_receivers_id") },
            inverseJoinColumns = { @JoinColumn(name = "user_id") }
    )
    private List<User> receivers = new ArrayList<>();
    

    public String toString() {
        return "Annotation " + annotationIdent + " shared by " + sender;
    }
    
    public void setAnnotation(AnnotationDomain annotation) {
        annotationClassName = annotation.getClass().getName();
        annotationIdent = annotation.getId();
    }
    

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        SharedAnnotation sharedAnnotation = this;
        sharedAnnotation.id = json.getJSONAttrLong("id",null);

        Long annotationId = json.getJSONAttrLong("annotationIdent", -1L);
        String annotationClassName = json.getJSONAttrStr("annotationClassName");
        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(entityManager, annotationId, annotationClassName);

        sharedAnnotation.setAnnotation(annotation);

        for (Long receiver : json.getJSONAttrListLong("receivers", new ArrayList<>())) {
            User user = entityManager.find(User.class, receiver);
            if (user != null) {
                sharedAnnotation.receivers.add(user);
            }
        }

        sharedAnnotation.comment = json.getJSONAttrStr("comment", false);
        sharedAnnotation.created = json.getJSONAttrDate("created");
        sharedAnnotation.updated = json.getJSONAttrDate("updated");
        
        sharedAnnotation.sender = (User) json.getJSONAttrDomain(entityManager, "sender", new User(), false);
        
        return sharedAnnotation;
    }

    public User userDomainCreator() {
        return sender;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        SharedAnnotation sharedAnnotation = (SharedAnnotation)domain;
        returnArray.put("annotationIdent", sharedAnnotation.getAnnotationIdent());
        returnArray.put("annotationClassName", sharedAnnotation.getAnnotationClassName());
        
        returnArray.put("comment", sharedAnnotation.getComment());
        returnArray.put("sender", (sharedAnnotation.getSender()!=null ? sharedAnnotation.getSender().getId() : null));
        returnArray.put("senderName", (sharedAnnotation.getSender()!=null ? sharedAnnotation.getSender().toString() : null));

        returnArray.put("receivers", sharedAnnotation.receivers.stream().map(x -> x.getId()).collect(Collectors.toList()));
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
        GenericCytomineDomainContainer genericCytomineDomainContainer = new GenericCytomineDomainContainer();
        genericCytomineDomainContainer.setId(annotationIdent);
        genericCytomineDomainContainer.setContainerClass(annotationClassName);
        return genericCytomineDomainContainer;
    }

}
