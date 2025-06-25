package be.cytomine.utils.filters;

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
import be.cytomine.exceptions.WrongArgumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SpecificationBuilder {


    public static <T extends CytomineDomain> Specification<T> getSpecificationFromFilters(List<SearchParameterEntry> filters){
        if (filters.size() > 0) {
            Specification<T> specification = createSpecification(filters.get(0));
            if (filters.size() > 1) {
                for (int i = 1 ; i<filters.size() ; i++) {
                    specification = specification.and(createSpecification(filters.get(i)));
                }
            }
            return specification;
        } else {
           return Specification.where(null);
        }
    }

    public static <T extends CytomineDomain> Specification<T> createSpecification(SearchParameterEntry input) {
        switch (input.getOperation()){

            case equals:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get(input.getProperty()),
                                castToRequiredType(root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));
            case nequals:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.notEqual(root.get(input.getProperty()),
                                castToRequiredType(root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));
            case gte:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.ge(root.get(input.getProperty()),
                                (Number) castToRequiredType(
                                        root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));
            case lte:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.le(root.get(input.getProperty()),
                                (Number) castToRequiredType(
                                        root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));
            case like:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.like(root.get(input.getProperty()),
                                "%"+input.getValue()+"%");

            case ilike:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.like(criteriaBuilder.lower(root.get(input.getProperty())),
                                "%"+input.getValue().toString().toLowerCase()+"%");
            case in:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.in(root.get(input.getProperty()))
                                .value(castToRequiredType(
                                        root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));

            default:
                throw new RuntimeException("Operation not supported yet");
        }
    }

    public static Object castToRequiredType(Class fieldType, Object value) {
        log.debug("castToRequiredType " + fieldType.getName() + " value " + value);
        log.debug("value has type " + value.getClass().getName());
        if (value instanceof List) {
            log.debug("value is a list");
            return ((List)value).stream().map(x -> castToRequiredType(fieldType, x)).collect(Collectors.toList());
        }

        if(fieldType.isAssignableFrom(Double.class)) {
            return Double.valueOf(value.toString());
        } else if(fieldType.isAssignableFrom(String.class)) {
            return String.valueOf(value.toString());
        }else if(fieldType.isAssignableFrom(Integer.class)) {
            return Integer.valueOf(value.toString());
        } else if(fieldType.isAssignableFrom(Long.class)) {
            log.debug("value is a long");
            return Long.valueOf(value.toString());
        }else if(Enum.class.isAssignableFrom(fieldType)) {
            return Enum.valueOf(fieldType, value.toString());
        } else if(CytomineDomain.class.isAssignableFrom(fieldType)) {
            log.debug("CytomineDomain");
            if (value instanceof Long) {
                log.debug("Long");
                return value;
            }
            if (value instanceof CytomineDomain) {
                log.debug("Object");
                return ((CytomineDomain) value);
            }
        }
        throw new WrongArgumentException("Type " + value.getClass().getName() + " not supported for filter " + fieldType);
    }

}
