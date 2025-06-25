package be.cytomine.repository.security;

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

import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AclRepository extends JpaRepository<User, Long> {



    @Query(value = "select count(user) from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, User as user "+
            "where aclObjectId.objectId = :projectId and aclEntry.aclObjectIdentity = aclObjectId and aclEntry.sid = aclSid and aclSid.sid = user.username " +
            "and user.id = :userId")
    Long countEntries(long projectId, long userId);


    @Query(value = "SELECT mask FROM acl_object_identity aoi, acl_sid sid, acl_entry ae " +
            "WHERE aoi.object_id_identity = :domainId " +
            "AND sid.sid = :username "  +
            "AND ae.acl_object_identity = aoi.id "+
            "AND ae.sid = sid.id ", nativeQuery = true)
    List<Integer> listMaskForUsers(Long domainId, String username);

    @Query(value = "SELECT mask FROM acl_object_identity aoi, acl_sid sid, acl_entry ae " +
            "WHERE aoi.object_id_identity = :domainId " +
            "AND sid.sid = :humanUsername "  +
            "AND ae.acl_object_identity = aoi.id "+
            "AND ae.sid = sid.id ", nativeQuery = true)
    List<Integer> listMaskForUsers(Long domainId);

    @Query(value = "SELECT id FROM acl_object_identity WHERE object_id_identity = :domainId", nativeQuery = true)
    Long getAclObjectIdentityFromDomainId(Long domainId);

    @Query(value = "SELECT id FROM acl_sid WHERE sid = :username", nativeQuery = true)
    Long getAclSidFromUsername(String username);

    @Query(value = "SELECT id FROM acl_entry WHERE acl_object_identity = :aclObjectIdentity AND mask = :mask AND sid= :sid", nativeQuery = true)
    Long getAclEntryId(Long aclObjectIdentity, Long sid, Integer mask);

    @Query(value = "SELECT max(ace_order) FROM acl_entry WHERE acl_object_identity = :aclObjectIdentity", nativeQuery = true)
    Integer getMaxAceOrder(Long aclObjectIdentity);

    @Modifying
    @Query(value = "INSERT INTO acl_entry(id,ace_order,acl_object_identity,audit_failure,audit_success,granting,mask,sid) " +
            "VALUES(nextval('hibernate_sequence'),:aceOrder,:aclObjectIdentity,false,false,true,:mask,:sid)", nativeQuery = true)
    void insertAclEntry(Integer aceOrder, Long aclObjectIdentity, Integer mask, Long sid);

    @Modifying
    @Query(value = "INSERT INTO acl_object_identity(id,object_id_class,entries_inheriting,object_id_identity,owner_sid,parent_object) " +
            "VALUES (nextval('hibernate_sequence'),:objectIdClass,true,:domainId,:sid,null)", nativeQuery = true)
    void insertAclObjectIdentity(Long objectIdClass, Long domainId, Long sid);

    @Query(value = "SELECT id FROM acl_sid WHERE sid = :username", nativeQuery = true)
    Long getAclSid(String username);

    @Modifying
    @Query(value = "INSERT INTO acl_sid(id,principal,sid) VALUES(nextval('hibernate_sequence'),true,:username)", nativeQuery = true)
    void insertAclSid(String username);

    @Query(value = "SELECT id FROM acl_class WHERE class = :className", nativeQuery = true)
    Long getAclClassId(String className);

    @Modifying
    @Query(value = "INSERT INTO acl_class(id,class) VALUES(nextval('hibernate_sequence'),:className)", nativeQuery = true)
    void insertAclClassId(String className);

    @Modifying
    @Query(value = "DELETE FROM acl_entry WHERE acl_object_identity = ? AND mask = ? AND sid = ?", nativeQuery = true)
    void deleteAclEntry(Long aclObjectIdentity, int mask, Long sid);

    @Query(value = "select user from AclObjectIdentity as aclObjectId, AclSid as aclSid, User as user where aclObjectId.objectId = :domainId and aclObjectId.ownerSid = aclSid and aclSid.sid = user.username")
    List<User> listCreators(Long domainId);

}
