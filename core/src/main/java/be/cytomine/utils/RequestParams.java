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

import java.util.HashMap;

public class RequestParams extends HashMap<String, String> {

    public boolean isTrue(String key) {
        return !isNull(key) && get(key).equals("true");
    }

    public boolean isNull(String key) {
        return get(key)==null;
    }

    public boolean isValue(String key, String value) {
        return !isNull(key) && get(key).equals(value);
    }

    public boolean getWithImageGroup() { return get("withImageGroup").equals("true"); }

    public Long getOffset() {
        return Long.parseLong(get("offset"));
    }

    public Long getMax() {
        return Long.parseLong(get("max"));
    }

    public String getSort() {
        return get("sort");
    }

    public String getOrder() {
        return get("order");
    }
}
