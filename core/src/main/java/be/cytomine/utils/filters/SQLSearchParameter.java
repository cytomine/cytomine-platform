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
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import org.springframework.util.ReflectionUtils;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.utils.filters.SearchOperation.ilike;
import static be.cytomine.utils.filters.SearchOperation.like;


public class SQLSearchParameter {

    //
    public static List<SearchParameterEntry> getDomainAssociatedSearchParameters(Class<? extends CytomineDomain> domain, List<SearchParameterEntry> searchParameters, EntityManager entityManager) {
        if(searchParameters==null) {
            return new ArrayList();
        }

        List<SearchParameterEntry> result = new ArrayList();
        List<SearchParameterEntry> translated = new ArrayList<>();

        for (SearchParameterEntry parameter : searchParameters) {

            if (parameter.operation.equals(ilike) || parameter.operation.equals(like)) {
                parameter.value = "%" + parameter.value + "%";
            }

            Field field = ReflectionUtils.findField(domain, parameter.property);

            if (field!=null) {
                Object value = convertSearchParameter(field.getType(), parameter.getValue(), entityManager);

                result.add(new SearchParameterEntry(field.getName(), parameter.operation, value));
                translated.add(parameter);
            }

            List classes = Arrays.asList(AbstractImage.class, ImageInstance.class);
            if (parameter.property.equals("include") && classes.contains(domain)) {
                result.add(new SearchParameterEntry(
                    domain == AbstractImage.class ? "id" : "base_image_id",
                    parameter.operation,
                    convertSearchParameter(Long.class, parameter.getValue(), entityManager))
                );

                translated.add(parameter);
            }
        }

        searchParameters.removeAll(translated);

        return result;
    }





    public static Object convertSearchParameter(Class type, Object parameter, EntityManager entityManager){

        if(parameter == null || parameter.equals("null")) {
            return null;
        }
        if(parameter instanceof List || parameter.getClass().isArray()) {
            return ((List)parameter).stream().map(x -> convertSearchParameter(type, x, entityManager)).collect(Collectors.toList());
        }

        Object output = null;

        if ((type == Integer.class || type == int.class) && !(parameter instanceof Integer)) {
            output = Integer.parseInt(parameter.toString());
        } else if ((type == Long.class || type == long.class) && !(parameter instanceof Long)) {
            output = Long.parseLong(parameter.toString());
        } else if ((type == Double.class || type == double.class) && !(parameter instanceof Double)) {
            output = Double.parseDouble(parameter.toString());
        } else if (type == Date.class) {
            output = new Date(Long.parseLong(parameter.toString()));
        } else if (CytomineDomain.class.isAssignableFrom(type)) {
            output = entityManager.find(type, Long.parseLong(parameter.toString()));
        } else {
            output = parameter;
        }
        return output;
    }

    private static Object replaceJocker(Object value) {
        if (value!=null && value instanceof String) {
            return ((String)value).replaceAll("\\*", "%");
        }
        return value;
    }


    public static SearchParameterProcessed searchParametersToSQLConstraints(List<SearchParameterEntry> parameters) {
        for (SearchParameterEntry parameter : parameters){
            parameter.property = fieldNameToSQL(parameter.property);

            if(parameter.value instanceof Date){
                parameter.value = ((Date) parameter.value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }

            String sql = "";
            switch(parameter.operation){
                case equals:
                    if(parameter.value != null) sql = parameter.property+" = :"+parameter.property.replaceAll("\\.","_");
                    else sql = parameter.property+" IS NULL ";
                    break;
                case nequals:
                    if(parameter.value != null) sql = parameter.property+" != :"+parameter.property.replaceAll("\\.","_");
                    else sql = parameter.property+" IS NOT NULL ";
                    break;
                case like:
                    sql = parameter.property+" LIKE :"+parameter.property.replaceAll("\\.","_");
                    break;
                case ilike:
                    sql = parameter.property+" ILIKE :"+parameter.property.replaceAll("\\.","_");
                    break;
                case lte:
                    sql = parameter.property+" <= :"+parameter.property.replaceAll("\\.","_");
                    break;
                case gte:
                    sql = parameter.property+" >= :"+parameter.property.replaceAll("\\.","_");
                    break;
                case in:
                    if(parameter.value == null || parameter.value == "null") {
                        sql = parameter.property+" IS NULL ";
                        break;
                    }

                    if(!parameter.value.getClass().isArray() && !(parameter.value instanceof List)){
                        parameter.value = List.of(parameter.value);
                    }

                    parameter.value = ((List)parameter.value).stream().distinct().collect(Collectors.toList());
                    List values = (List)parameter.value;

                    if(values.size() == 1 && (values.get(0) == null || (values.get(0).equals("null")))) {
                        sql = parameter.property+" IS NULL ";
                        break;
                    }

                    sql = parameter.property+" IN (";


                    int count = ((List)values.stream().filter(x -> x != null && !x.equals("null")).collect(Collectors.toList())).size();

                    List<String> strings = new ArrayList<>();
                    for (int i=1; i<=count; i++) {
                        strings.add(":"+parameter.property.replaceAll("\\.","_")+"_"+i);
                    }
                    sql = sql + String.join(",", strings);
                    sql += ") ";

                    if(values.contains(null) || values.contains("null")){
                        parameter.value = values.stream().filter(x -> x != null && !x.equals("null")).collect(Collectors.toList());
                        sql = "("+sql+" OR "+parameter.property+" IS NULL) ";
                    } else {
                        break;
                    }
                    break;
            }
            parameter.sql = sql;
            parameter.sqlParameter = new HashMap<>();

            if(parameter.value!=null && (parameter.value.getClass().isArray() || (parameter.value instanceof List))){
                List values = (List)parameter.value;
                for(int i=1; i<= values.size(); i++) {
                    parameter.sqlParameter.put(parameter.property.replaceAll("\\.","_")+"_"+i, replaceJocker(values.get(i-1)));
                }
            } else {
                parameter.sqlParameter.put(parameter.property.replaceAll("\\.","_"), replaceJocker(parameter.value));
            }
        }

        //remove parameters not injected into sql (as IS NULL)
        for (SearchParameterEntry parameter : parameters) {
            Map<String, Object> filtered = new HashMap<>();
            for (Map.Entry<String, Object> entry : parameter.sqlParameter.entrySet()) {
                if (parameter.sql.contains(entry.getKey())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            parameter.sqlParameter = filtered;
            if(parameter.sqlParameter.size() == 0) {
                parameter.sqlParameter = null;
            }
        }

        SearchParameterProcessed searchParameters = new SearchParameterProcessed();
        searchParameters.setData(parameters);
        searchParameters.setSqlParameters(new HashMap<>());

        List<Map<String, Object>> sqlParameters = new ArrayList<>();
        for (SearchParameterEntry datum : searchParameters.data) {
            if (datum.sqlParameter != null) {
                sqlParameters.add(datum.sqlParameter);
            }
        }

        Map<String, Integer> parametersCount = new HashMap<>();
        for (Map<String, Object> sqlParameter : sqlParameters) {
            for (String key : sqlParameter.keySet()) {
                Integer numberOfEntries = parametersCount.getOrDefault(key, 0);
                parametersCount.put(key, numberOfEntries + 1);
            }
        };

        //if a same property is used multiple times
        for (Map.Entry<String, Integer> properties : parametersCount.entrySet()) {
            if (properties.getValue() > 1) {
                List<SearchParameterEntry> duplicatedEntries = searchParameters.data.stream().filter( x -> properties.getKey().equals(x.property.replaceAll("\\.","_"))).collect(Collectors.toList());
                for (int i = 0 ; i<duplicatedEntries.size() ; i++) {
                    String oldName = duplicatedEntries.get(i).sqlParameter.keySet().iterator().next();
                    String newName = oldName+"_"+i;
                    duplicatedEntries.get(i).sql = duplicatedEntries.get(i).sql.replace(":"+oldName, ":"+newName);
                    duplicatedEntries.get(i).sqlParameter.put(newName, duplicatedEntries.get(i).sqlParameter.remove(oldName));
                }

            }
        }

        for (Map<String, Object> sqlParameter : sqlParameters) {
            searchParameters.sqlParameters.putAll(sqlParameter);
        }

        return searchParameters;
    }

    public static String fieldNameToSQL(String field) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return field.replaceAll(regex, replacement).toLowerCase();
    }





}
