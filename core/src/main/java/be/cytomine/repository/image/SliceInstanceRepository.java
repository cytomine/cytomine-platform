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

import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the abstract image entity.
 */
@Repository
public interface SliceInstanceRepository extends JpaRepository<SliceInstance, Long>, JpaSpecificationExecutor<SliceInstance> {

    Optional<SliceInstance> findByBaseSliceAndImage(AbstractSlice abstractSlice, ImageInstance imageInstance);

    @EntityGraph(attributePaths = {"baseSlice", "project"})
    List<SliceInstance> findAllByImage(ImageInstance imageInstance);

    List<SliceInstance> findAllByBaseSlice(AbstractSlice abstractSlice);

    @Query("SELECT si " +
            "FROM SliceInstance si INNER JOIN FETCH si.baseSlice as bs " +
            "WHERE si.image = :imageInstance " +
            "AND si.baseSlice.channel = :c " +
            "AND si.baseSlice.zStack = :z " +
            "AND si.baseSlice.time = :t")
    Optional<SliceInstance> findByCZT(ImageInstance imageInstance, int c, int z, int t);

    @Query("SELECT si " +
            "FROM SliceInstance si INNER JOIN FETCH si.baseSlice as bs INNER JOIN bs.uploadedFile uf " +
            "WHERE si.image = :imageInstance " +
            "ORDER BY " +
            "   si.baseSlice.channel ASC, " +
            "   si.baseSlice.zStack ASC, " +
            "   si.baseSlice.time ASC ")
    List<SliceInstance> listByImageInstanceOrderedByCZT(ImageInstance imageInstance);


    @Query("SELECT si " +
            "FROM SliceInstance si INNER JOIN FETCH si.baseSlice as bs " +
            "WHERE si.image = :imageInstance " +
            "AND bs.time >= :baseSliceTime " +
            "AND bs.zStack >= :baseSliceZStack " +
            "AND bs.channel >= :baseSliceChannel " +
            "AND si.id <> :userAnnotationSliceId " +
            "ORDER BY " +
            "   bs.time ASC, " +
            "   bs.zStack ASC, " +
            "   bs.channel ASC ")
    List<SliceInstance> listByImageInstanceOrderedByTZC(ImageInstance imageInstance, Integer baseSliceTime, Integer baseSliceZStack, Integer baseSliceChannel, Long userAnnotationSliceId);

    boolean existsByBaseSlice(AbstractSlice abstractSlice);
}
