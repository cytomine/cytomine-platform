package be.cytomine.repository.meta;

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

import be.cytomine.domain.meta.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property>  {

    Optional<Property> findByKey(String key);

    Optional<Property> findByDomainIdentAndKey(Long domainIdent, String key);

    List<Property> findAllByDomainIdentAndKeyIn(Long domainIdent, Collection<String> keys);

    void deleteAllByDomainIdentAndKeyIn(Long domainIdent, Collection<String> keys);

    List<Property> findAllByDomainIdent(Long id);


    @Query(value ="SELECT * FROM property p WHERE p.domain_ident = :domainIdent AND NOT EXISTS ( SELECT 1 FROM unnest(STRING_TO_ARRAY(:excludedKeys, ';')) AS substr WHERE p.key LIKE (substr || '%'))", nativeQuery = true)
    List<Property> findByDomainIdentAndExcludedKeys(@Param("domainIdent") Long domainIdent, @Param("excludedKeys") String excludedKeys);


    void deleteAllByDomainIdent(Long id);
}
