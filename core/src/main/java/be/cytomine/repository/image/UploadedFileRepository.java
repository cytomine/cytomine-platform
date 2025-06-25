package be.cytomine.repository.image;

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

import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long>, JpaSpecificationExecutor<UploadedFile> {

    @Query(value = "SELECT uf.* " +
            "FROM uploaded_file uf " +
            "WHERE true " +
            "AND (:ignoreUserIdFilter IS TRUE OR uf.user_id = :userId) " +
            "AND (:ignoreParentIdFilter IS TRUE OR uf.parent_id = :parentId) " +
            "AND (:ignoreOnlyRootFilter IS TRUE OR (:onlyRoot IS FALSE OR uf.parent_id IS NULL)) " +
            "AND (:ignoreStorageIdsFilter IS TRUE OR (uf.storage_id IN (:storagesIds)))"
            , nativeQuery = true)
    Page<UploadedFile> search(
            Boolean ignoreUserIdFilter,
            Long userId,
            Boolean ignoreParentIdFilter,
            Long parentId,
            Boolean ignoreOnlyRootFilter,
            Boolean onlyRoot,
            Boolean ignoreStorageIdsFilter,
            List<Long> storagesIds,
            Pageable pageable
    );

    default Page<UploadedFile> search(Long userId, Long parentId, Boolean onlyRoot, List<Long> storagesIds, Pageable pageable) {
        return search(
                userId==null,
                Optional.ofNullable(userId).orElse(-1L),
                parentId==null,
                Optional.ofNullable(parentId).orElse(-1L),
                onlyRoot==null,
                Optional.ofNullable(onlyRoot).orElse(false),
                storagesIds==null,
                Optional.ofNullable(storagesIds).orElse(new ArrayList<>()),
                pageable);
    }

    Integer countByParent(UploadedFile domain);

    List<UploadedFile> findAllByParent(UploadedFile uploadedFile);

    void deleteAllByUser(User user);

    long countByStorage(Storage storage);
}
