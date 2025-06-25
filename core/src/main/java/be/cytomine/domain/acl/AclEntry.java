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

@Entity(name = "AclEntry")
@Table(name = "acl_entry")
@Immutable
public class AclEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "ace_order", nullable = false, unique = false)
    private Integer aceOrder;

    @Column(name = "mask", nullable = false, unique = false)
    private Integer mask;

    @Column(name = "granting", nullable = false, unique = false)
    private Boolean granting;

    @Column(name = "audit_success", nullable = false, unique = false)
    private Boolean auditSuccess;

    @Column(name = "audit_failure", nullable = false, unique = false)
    private Boolean auditFailure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acl_object_identity", referencedColumnName = "id", nullable = false, unique = false, insertable = true, updatable = true)
    private AclObjectIdentity aclObjectIdentity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sid", referencedColumnName = "id", nullable = false, unique = false, insertable = true, updatable = true)
    private AclSid sid;

    public AclEntry() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAceOrder() {
        return aceOrder;
    }

    public void setAceOrder(Integer aceOrder) {
        this.aceOrder = aceOrder;
    }

    public Integer getMask() {
        return mask;
    }

    public void setMask(Integer mask) {
        this.mask = mask;
    }

    public Boolean getGranting() {
        return granting;
    }

    public void setGranting(Boolean granting) {
        this.granting = granting;
    }

    public Boolean getAuditSuccess() {
        return auditSuccess;
    }

    public void setAuditSuccess(Boolean auditSuccess) {
        this.auditSuccess = auditSuccess;
    }

    public Boolean getAuditFailure() {
        return auditFailure;
    }

    public void setAuditFailure(Boolean auditFailure) {
        this.auditFailure = auditFailure;
    }

    public AclObjectIdentity getAclObjectIdentity() {
        return aclObjectIdentity;
    }

    public void setAclObjectIdentity(AclObjectIdentity aclObjectIdentity) {
        this.aclObjectIdentity = aclObjectIdentity;
    }

    public AclSid getSid() {
        return sid;
    }

    public void setSid(AclSid sid) {
        this.sid = sid;
    }

    @Override
    public String toString() {
        return "AclEntry [id=" + id + ", aceOrder=" + aceOrder + ", mask="
                + mask + ", granting=" + granting + ", auditSuccess="
                + auditSuccess + ", auditFailure=" + auditFailure
                + ", aclObjectIdentity=" + aclObjectIdentity + ", sid=" + sid
                + "]";
    }

}
