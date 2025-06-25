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

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.UploadedFile;
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
public interface AbstractSliceRepository extends JpaRepository<AbstractSlice, Long>, JpaSpecificationExecutor<AbstractSlice> {

    List<AbstractSlice> findAllByImage(AbstractImage abstractImage);

    List<AbstractSlice> findAllByUploadedFile(UploadedFile uploadedFile);

    @Query("SELECT asl FROM AbstractSlice asl WHERE asl.image = :image AND asl.channel = :channel  AND asl.zStack = :zStack AND asl.time = :time")
    Optional<AbstractSlice> findByImageAndChannelAndZStackAndTime(AbstractImage image, Integer channel, Integer zStack, Integer time);

    List<AbstractSlice> findAllByImageAndChannel(AbstractImage image, int index);
}
