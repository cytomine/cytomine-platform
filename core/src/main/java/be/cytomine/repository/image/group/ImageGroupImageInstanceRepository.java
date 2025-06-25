package be.cytomine.repository.image.group;

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

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.image.group.ImageGroupImageInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageGroupImageInstanceRepository extends JpaRepository<ImageGroupImageInstance, Long>, JpaSpecificationExecutor<ImageGroupImageInstance> {

    List<ImageGroupImageInstance> findAllByImage(ImageInstance image);

    List<ImageGroupImageInstance> findAllByGroup(ImageGroup group);

    Optional<ImageGroupImageInstance> findByGroupAndImage(ImageGroup group, ImageInstance imageInstance);

    void deleteAllByGroup(ImageGroup group);
}
