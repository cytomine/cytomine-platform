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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class StringUtils {

    public static String getBlankIfNull(String str) {
        return (str == null ? "" : str);
    }


    public static boolean isNotBlank(String str) {
        return str != null && !str.equals("");
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().equals("");
    }

    public static String obscurify(String property, int numberOfCharsToKeepOnEachSide) {
        if (property==null) {
            return "<NULL>";
        }
        if (property.trim().isEmpty()) {
            return "<EMPTY>";
        }
        if (numberOfCharsToKeepOnEachSide*2<property.length()) {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i< property.length()-(numberOfCharsToKeepOnEachSide*2);i++) {
                buffer.append("*");
            }
            return property.substring(0,numberOfCharsToKeepOnEachSide)+ buffer.toString() + property.substring(property.length()-numberOfCharsToKeepOnEachSide,property.length());
        } else {
            return property;
        }
    }

    public static List<Long> extractListFromParameter(String parameter) {
        if (parameter==null ||  parameter.isEmpty()) {
            return null;
        }
        return Arrays.stream(parameter.replaceAll("_",",").split(",")).map(Long::parseLong)
                .collect(Collectors.toList());
    }

    public static String decimalFormatter(Object value){
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(value);
    }

    public static Map<String, Object> keysToCamelCase(Map<String, Object> map) {
        HashMap<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(SQLUtils.toCamelCase(entry.getKey()), entry.getValue());
        }
        return result;
    }

    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    public static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    public static String urlEncodeUTF8(Map<?,?> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?,?> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                    urlEncodeUTF8(entry.getKey().toString()),
                    urlEncodeUTF8(entry.getValue().toString())
            ));
        }
        return sb.toString();
    }
}
