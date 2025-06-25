package be.cytomine.utils;

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
import be.cytomine.dto.json.JsonInput;
import be.cytomine.exceptions.ServerException;
import be.cytomine.exceptions.WrongArgumentException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.utils.DateUtils.MONGO_DB_FORMAT;

public class JsonObject extends HashMap<String, Object> implements JsonInput {

    public JsonObject() {

    }

    public JsonObject(Map<String, Object> data) {
        super(data);
    }



    public boolean isMissing(String key) {
        return !this.containsKey(key) || this.get(key)==null;
    }


    public JsonObject withChange(String key, Object value) {
        if (value == null) {
            this.remove(key);
        } else {
            this.put(key, value);
        }
        return this;
    }

    public static JsonObject of() {
        return new JsonObject();
    }

    public static JsonObject of(String key, Object value) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(key, value);
        return jsonObject;
    }

    public static JsonObject of(String key, Object value, String key2, Object value2) {
        JsonObject jsonObject = of(key, value);
        jsonObject.put(key2, value2);
        return jsonObject;
    }

    public static JsonObject of(String key, Object value, String key2, Object value2, String key3, Object value3) {
        JsonObject jsonObject = of(key, value, key2, value2);
        jsonObject.put(key3, value3);
        return jsonObject;
    }

    public static JsonObject of(String key, Object value, String key2, Object value2, String key3, Object value3, String key4, Object value4) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(key, value);
        jsonObject.put(key2, value2);
        jsonObject.put(key3, value3);
        jsonObject.put(key4, value4);
        return jsonObject;
    }
    public static JsonObject of(String key, Object value, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(key, value);
        jsonObject.put(key2, value2);
        jsonObject.put(key3, value3);
        jsonObject.put(key4, value4);
        jsonObject.put(key5, value5);
        return jsonObject;
    }

    public static JsonObject of(String key, Object value, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5, String key6, Object value6) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(key, value);
        jsonObject.put(key2, value2);
        jsonObject.put(key3, value3);
        jsonObject.put(key4, value4);
        jsonObject.put(key5, value5);
        jsonObject.put(key6, value6);
        return jsonObject;
    }

    public static JsonObject of(String key, Object value, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5, String key6, Object value6, String key7, Object value7) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(key, value);
        jsonObject.put(key2, value2);
        jsonObject.put(key3, value3);
        jsonObject.put(key4, value4);
        jsonObject.put(key5, value5);
        jsonObject.put(key6, value6);
        jsonObject.put(key7, value7);
        return jsonObject;
    }

    public String toJsonString() {
        return toJsonString(this);
    }

    public static String toJsonString(Object o) {
        try {
            return new ObjectMapper().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "invalid json string";
        }
    }

    public static Map<String, Object> toMap(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T toObject(String json, Class<? extends T> c) {
        try {
            return new ObjectMapper().readValue(json, c);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> toStringList(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<String>>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Map<String, Object>> toMapList(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<Map<String, Object>>>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static JsonObject toJsonObject(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<JsonObject>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getJSONAttrStr(String attr) {
        return getJSONAttrStr(attr, false, null);
    }

    public String getJSONAttrStr(String attr, boolean mandatory) {
        return getJSONAttrStr(attr, mandatory, null);
    }

    public String getJSONAttrStr(String attr, String defaultValue) {
        return getJSONAttrStr(attr, false, defaultValue);
    }

    public String getJSONAttrStr(String attr, boolean mandatory, String defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            if (mandatory && this.get(attr).equals("")) {
                throw new WrongArgumentException(attr + " must be set! value=" + this.get(attr));
            }
            return this.get(attr).toString();
        } else {
            if (mandatory) {
                throw new WrongArgumentException(attr + " must be set! value=" + this.get(attr));
            }
            return defaultValue;
        }
    }

    public Double getJSONAttrDouble(String attr) {
        return getJSONAttrDouble(attr, null);
    }

    public Double getJSONAttrDouble(String attr, Double defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            try {
                return Double.parseDouble(this.get(attr).toString());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public Integer getJSONAttrInteger(String attr, Integer defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            try {
                return Integer.parseInt(this.get(attr).toString());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public Integer getJSONAttrInteger(String attr) {
        return getJSONAttrInteger(attr, null);
    }


    public Long getJSONAttrLong(String attr, Long defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            try {
                return Long.parseLong(this.get(attr).toString());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public Long getJSONAttrLong(String attr) {
        return getJSONAttrLong(attr, null);
    }

    public Boolean getJSONAttrBoolean(String attr, Boolean defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            try {
                return Boolean.parseBoolean(this.get(attr).toString());
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public Date getJSONAttrDate(String attr) {
        if (this.get(attr) != null && this.get(attr) instanceof Date) {
          return (Date) this.get(attr);
        } if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            try {
                return new Date(Long.parseLong(this.get(attr).toString()));
            } catch (NumberFormatException exception) {
                try {
                    return MONGO_DB_FORMAT.parse(this.get(attr).toString());
                } catch(Exception ex) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    /**
     * Get attr domain value from json
     * Read domain thanks to domain argument class and its id (domain.read)
     * If mandatory flag is true, check if domain exists
     * @return  Value as Cytomine Domain
     */
    public CytomineDomain getJSONAttrDomain(EntityManager entityManager, String attr, CytomineDomain domain, boolean mandatory) {
        return getJSONAttrDomain(entityManager, attr, domain, "id", "Long", mandatory);
    }

    /**
     * Get attr domain value from json
     * Read domain thanks to domain argument, get the correct object thanks to value from column (type: columnType)
     * If mandatory flag is true, check if domain exists
     * @return  Value as Cytomine Domain
     */
    public CytomineDomain getJSONAttrDomain(EntityManager entityManager, String attr, CytomineDomain domain, String column, String columnType, boolean mandatory) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            CytomineDomain domainRead;

            if (this.get(attr) instanceof CytomineDomain) {
                return (CytomineDomain)this.get(attr);
            } else if(column.equals("id")) {
                domainRead = entityManager.find(domain.getClass(), Long.parseLong(this.get(attr).toString()));
            } else {
                String request = "SELECT d FROM " + domain.getClass().getName() + " d WHERE " + column + " = :value";
                Query query = entityManager.createQuery(request, domain.getClass());
                query.setParameter("value", convertValue(this.get(attr), columnType));
                List resultList = query.getResultList();
                domainRead = (resultList.size()>=1 ? (CytomineDomain) resultList.get(0) : null);
                if (domainRead == null) {
                    throw new WrongArgumentException(domain.getClass().getName() + " cannot be found with " + column + " = '" + convertValue(this.get(attr), columnType) + "'");
                }
            }
            if (domainRead == null) {
                throw new WrongArgumentException(attr + " was not found with id: " + this.get(attr));
            }
            return domainRead;
        } else {
            if (mandatory) {
                throw new WrongArgumentException(attr + " was not found with id: " + this.get(attr));
            }
            return null;
        }
    }

    static public Object convertValue(Object value, String type) {
        if(value.equals("null")) {
            return null;
        }else if (type.equals("String")) {
            return value.toString();
        } else if (type.equals("Long")) {
            return Long.parseLong(value.toString());
        }
        throw new ServerException("Type " + type + " not supported! See cytominedomain class");
    }

    public JsonObject extractProperty(String key) {
        Map<String, Object> values = (Map<String, Object>)this.get(key);
        return new JsonObject(values);
    }

    public List<Long> getJSONAttrListLong(String attr) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            if (this.get(attr) instanceof List) {
                return (List<Long>)((List)this.get(attr)).stream().map(x -> Long.parseLong(String.valueOf(x))).collect(Collectors.toList());
            } else if(this.get(attr) instanceof Long[]) {
                return Arrays.asList((Long[]) this.get(attr));
            } else if(this.get(attr) instanceof Integer[]) {
                return Arrays.asList((Integer[]) this.get(attr)).stream().map(Integer::longValue).toList();
            } else if(this.get(attr) instanceof Long) {
                return List.of(((Long)this.get(attr)));
            } else if(this.get(attr) instanceof Integer) {
                return List.of(((Integer)this.get(attr)).longValue());
            } else if(this.get(attr) instanceof String) {
                return Arrays.stream(this.get(attr).toString().split(",")).map(x -> Long.parseLong(String.valueOf(x))).collect(Collectors.toList());
            }
        }
        return null;
    }

    public List<Long> getJSONAttrListLong(String attr, List<Long> defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            if (this.get(attr) instanceof List) {
                return (List<Long>)((List)this.get(attr)).stream().map(x -> Long.parseLong(String.valueOf(x))).collect(Collectors.toList());
            } else if(this.get(attr) instanceof Long[]) {
                return Arrays.asList((Long[]) this.get(attr));
            } else if(this.get(attr) instanceof String) {
                return Arrays.stream(this.get(attr).toString().split(",")).map(x -> Long.parseLong(String.valueOf(x))).collect(Collectors.toList());
            }
        }
        return defaultValue;
    }

    public Map<String, String> getJSONAttrMapString(String attr, Map<String, String> defaultValue) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            return (Map<String, String>)get(attr);
        } else {
            return defaultValue;
        }
    }

    public JsonObject getJSONObject(String attr) {
        if (this.get(attr) != null && !this.get(attr).toString().equals("null")) {
            return new JsonObject(getJSONAttrMap(attr));
        } else {
            return new JsonObject();
        }
    }

    public Long getId() {
        return getJSONAttrLong("id");
    }

    public List<String> getJSONAttrListString(String attr) {
        return (List<String>) this.get(attr);
    }

    public List<Map<String, Object>> getJSONAttrListMap(String attr) {
        return (List<Map<String, Object>>) this.get(attr);
    }

    public Map<String, Object> getJSONAttrMap(String attr) {
        return (Map<String, Object>) this.get(attr);
    }
}
