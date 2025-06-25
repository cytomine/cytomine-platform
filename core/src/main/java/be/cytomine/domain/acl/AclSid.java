package be.cytomine.domain.acl;

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

import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity(name = "AclSid")
@Table(name = "acl_sid")
@Immutable
public class AclSid implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "principal", nullable = false, unique = false)
    private Boolean principal;

    @Column(name = "sid", length = 100, nullable = false, unique = false)
    private String sid;

    public AclSid() {
    }

    public AclSid(Long id, Boolean principal, String sid) {
        setId(id);
        setPrincipal(principal);
        setSid(sid);
    }

    public AclSid flat() {
        return new AclSid(getId(), getPrincipal(), getSid());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getPrincipal() {
        return principal;
    }

    public void setPrincipal(Boolean principal) {
        this.principal = principal;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    @Override
    public String toString() {
        return "AclSid [id=" + id + ", principal=" + principal + ", sid=" + sid
                + "]";
    }

}
