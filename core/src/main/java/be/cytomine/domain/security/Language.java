package be.cytomine.domain.security;

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

import java.util.HashMap;
import java.util.Map;

public enum Language {
    // see https://fr.wikipedia.org/wiki/Liste_des_codes_ISO_639-1
    ARABIC("AR"),
    GERMAN("DE"),
    GREEK("EL"),
    ENGLISH("EN"),
    FRENCH("FR"),
    DUTCH("NL"),
    SPANISH("ES");

    private final String code;

    Language(String code) {this.code = code;}

    private static final Map<String,Language> map;

    static {
        map = new HashMap<>();
        for (Language l : Language.values()) {
            map.put(l.code, l);
        }
    }
    public static Language findByCode(String c) {
        return map.get(c);
    }

    @Override
    public String toString(){
        return this.code;
    }
}
