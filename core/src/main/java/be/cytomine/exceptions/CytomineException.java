package be.cytomine.exceptions;

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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: lrollus
 * Date: 17/11/11
 * This exception is the top exception for all cytomine exception
 * It store a message and a code, corresponding to an HTTP code
 */
public abstract class CytomineException extends RuntimeException {



    /**
     * Http code for an exception
     */
    public int code;

    /**
     * Message for exception
     */
    public String msg;

    /**
     * Values of the exception
     */
    public Map<Object, Object> values;


    /**
     * Headers to be sent in the response (can contain information on the exception
     * or how the client has to treat the exception).
     */
    public Map<String, String> headers;

    /**
     * Message map with this exception
     * @param msg Message
     * @param code Http code
     */
    public CytomineException(String msg, int code) {
        this(msg,code,new HashMap<>(), new LinkedHashMap<String, String>(), null);
    }

    public CytomineException(String msg, int code, Throwable cause) {
        this(msg,code,new HashMap<>(), new LinkedHashMap<String, String>(), cause);
    }


    public CytomineException(String msg, int code, Map<Object, Object> values) {
        this(msg, code, values, new LinkedHashMap<String, String>(), null);
    }
    public CytomineException(String msg, int code, Map<Object, Object> values, Map<String, String> headers, Throwable cause) {
        super(msg, cause);
        this.msg=msg;
        this.code = code;
        this.values = values;
        this.headers = headers;
    }

    public String toString() {
        return this.msg;
    }

    public Map<Object, Object> getValues() {
        return values;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
