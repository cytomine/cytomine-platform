package be.cytomine.service;

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
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static be.cytomine.utils.ClassUtils.getClassName;

@Service
@Transactional()
public class ResponseService {

    @Autowired
    MessageSource messageSource;

    public CommandResponse createResponseMessage(CytomineDomain object, List<Object> messageParams, boolean printMessage, String commandType) {
        return createResponseMessage(object, messageParams, printMessage, commandType, null);
    }

    /**
     * Create response message structure for a command result
     * E.g. if we try to add a new annotation "object"
     * @param object Object updated (add/edit/delete) by command  (e.g. annotation x)
     * @param messageParams Params for i18n message (e.g. annotation id, annotation image id)
     * @param printMessage Flag for client, indicate if client must print or not a confirmation message
     * @param commandType Command type: add, edit or delete
     * @param additionalCallbackParams (optional): call back to append in response for client (e.g. image id as callback, to refresh image view in web UI client)
     * @return Response stucture
     */
    public CommandResponse createResponseMessage(CytomineDomain object, List<Object> messageParams, boolean printMessage, String commandType, Map<String, Object> additionalCallbackParams) {
        String objectName = getClassName(object);
        String command = "be.cytomine." + commandType + objectName + "Command";
        String idName = objectName.toLowerCase() + "ID"; //termID, annotationID,...
        String id = String.valueOf(object.getId());
        HashMap<String, Object> paramsCallback = new HashMap<>();
        paramsCallback.put("method", command);
        paramsCallback.put(idName, id);
        if (additionalCallbackParams!=null) {
            paramsCallback.putAll(additionalCallbackParams);
        }

        //load message from i18n filel
        String message = messageSource.getMessage(command, messageParams.toArray(), Locale.ENGLISH);

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("message", message);
        params.put("callback", paramsCallback);
        params.put("printMessage", printMessage);
        params.put(objectName.toLowerCase(), JsonObject.toMap(object.toJSON()));     //Project.getDataFromDomain(object)

        CommandResponse commandResponse = new CommandResponse();
        commandResponse.setData(params);
        commandResponse.setStatus(200);
        commandResponse.setObject(object);
        return commandResponse;
    }

    public Map<String, Object> createResponseData(Boolean success, String messageKey, HashMap<String, Object> callback, boolean printMessage) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("success", success);
        params.put("message", messageSource.getMessage(messageKey, new Object[]{}, Locale.ENGLISH));
        params.put("callback", callback);
        params.put("printMessage", printMessage);
        return params;
    }

}
