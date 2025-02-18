package be.cytomine.appengine.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import be.cytomine.appengine.dto.inputs.task.StateAction;
import be.cytomine.appengine.dto.inputs.task.TaskDescription;
import be.cytomine.appengine.dto.inputs.task.TaskInput;
import be.cytomine.appengine.dto.inputs.task.TaskOutput;
import be.cytomine.appengine.dto.inputs.task.TaskRun;
import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;
import be.cytomine.appengine.models.task.Input;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.states.TaskRunState;

@Component
public class ApiClient {

    private final RestTemplate restTemplate;

    private String baseUrl;

    private String port;

    private File writeToFile(String filename, String suffix, byte[] content) {
        try {
            File tempFile = File.createTempFile(filename, suffix);
            tempFile.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(content);
            }

            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public <T> ResponseEntity<T> get(String url, Class<T> responseType) {
        return restTemplate.getForEntity(url, responseType);
    }

    public <T> ResponseEntity<T> get(String url, ParameterizedTypeReference<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.GET, null, responseType);
    }

    public <T> ResponseEntity<T> post(String url, Object body, Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body), responseType);
    }

    public <T> ResponseEntity<T> postJson(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
    }

    public <T> ResponseEntity<T> postData(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
    }

    public <T> ResponseEntity<T> put(String url, HttpEntity<Object> entity, Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
    }

    public <T> ResponseEntity<T> put(String url, HttpEntity<Object> entity, ParameterizedTypeReference<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
    }

    public ResponseEntity<String> checkHealth() {
        return get("http://localhost:" + port + "/actuator/health", String.class);
    }

    public TaskDescription uploadTask(File task) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("task", new FileSystemResource(task));

        return postData(baseUrl + "/tasks", body, TaskDescription.class).getBody();
    }

    public TaskDescription getTask(String namespace, String version) {
        return get(baseUrl + "/tasks/" + namespace + "/" + version, TaskDescription.class).getBody();
    }

    public TaskDescription getTask(String uuid) {
        return get(baseUrl + "/tasks/" + uuid, TaskDescription.class).getBody();
    }

    public List<TaskDescription> getTasks() {
        return get(baseUrl + "/tasks", new ParameterizedTypeReference<List<TaskDescription>>() {}).getBody();
    }

    public File getTaskDescriptor(String namespace, String version) {
        String url = baseUrl + "/tasks/" + namespace + "/" + version + "/descriptor.yml";
        byte[] resource = get(url, byte[].class).getBody();

        return writeToFile("descriptor-", null, resource);
    }

    public File getTaskDescriptor(String uuid) {
        String url = baseUrl + "/tasks/" + uuid + "/descriptor.yml";
        byte[] resource = get(url, byte[].class).getBody();

        return writeToFile("descriptor-", null, resource);
    }

    public List<TaskInput> getTaskInputs(String namespace, String version) {
        String url = baseUrl + "/tasks/" + namespace + "/" + version + "/inputs";
        return get(url, new ParameterizedTypeReference<List<TaskInput>>() {}).getBody();
    }

    public List<TaskInput> getTaskInputs(String uuid) {
        String url = baseUrl + "/tasks/" + uuid + "/inputs";
        return get(url, new ParameterizedTypeReference<List<TaskInput>>() {}).getBody();
    }

    public List<TaskOutput> getTaskOutputs(String namespace, String version) {
        String url = baseUrl + "/tasks/" + namespace + "/" + version + "/outputs";
        return get(url, new ParameterizedTypeReference<List<TaskOutput>>() {}).getBody();
    }

    public List<TaskOutput> getTaskOutputs(String uuid) {
        String url = baseUrl + "/tasks/" + uuid + "/outputs";
        return get(url, new ParameterizedTypeReference<List<TaskOutput>>() {}).getBody();
    }

    public List<Input> getInputs(String namespace, String version) {
        String url = baseUrl + "/tasks/" + namespace + "/" + version + "/inputs";
        return get(url, new ParameterizedTypeReference<List<Input>>() {}).getBody();
    }

    public List<Input> getInputs(String uuid) {
        String url = baseUrl + "/tasks/" + uuid + "/inputs";
        return get(url, new ParameterizedTypeReference<List<Input>>() {}).getBody();
    }

    public List<Output> getOutputs(String namespace, String version) {
        String url = baseUrl + "/tasks/" + namespace + "/" + version + "/outputs";
        return get(url, new ParameterizedTypeReference<List<Output>>() {}).getBody();
    }

    public List<Output> getOutputs(String uuid) {
        String url = baseUrl + "/tasks/" + uuid + "/outputs";
        return get(url, new ParameterizedTypeReference<List<Output>>() {}).getBody();
    }

    public TaskRun createTaskRun(String namespace, String version) {
        return post(baseUrl + "/tasks/" + namespace + "/" + version + "/runs", null, TaskRun.class).getBody();
    }

    public TaskRun createTaskRun(String uuid) {
        return post(baseUrl + "/tasks/" + uuid + "/runs", null, TaskRun.class).getBody();
    }

    public TaskRun getTaskRun(String uuid) {
        return get(baseUrl + "/task-runs/" + uuid, TaskRun.class).getBody();
    }

    public JsonNode provisionInput(String uuid, String parameterName, String type, String value) {
        HttpEntity<Object> entity = null;
        if (type.equals("image") || type.equals("wsi") || type.equals("file")) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(value.getBytes()) {
                @Override
                public String getFilename() {
                    return "file.txt";
                }
            };
            body.add("file", fileResource);

            entity = new HttpEntity<>(body, headers);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonNode = mapper.valueToTree(
                TaskTestsUtils.createProvision(parameterName, type, value)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            entity = new HttpEntity<>(jsonNode, headers);
        }

        return put(baseUrl + "/task-runs/" + uuid + "/input-provisions/" + parameterName, entity, JsonNode.class).getBody();
    }

    public List<JsonNode> provisionMultipleInputs(String uuid, List<ObjectNode> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        return put(baseUrl + "/task-runs/" + uuid + "/input-provisions", entity, new ParameterizedTypeReference<List<JsonNode>>() {}).getBody();
    }

    public ResponseEntity<StateAction> updateState(String uuid, TaskRunState state) {
        String body = "{\"desired\": \"" + state.toString() + "\"}";
        return postJson(baseUrl + "/task-runs/" + uuid + "/state-actions", body, StateAction.class);
    }

    public File getTaskRunInputsArchive(String uuid) {
        String url = baseUrl + "/task-runs/" + uuid + "/inputs.zip";
        byte[] resource = get(url, byte[].class).getBody();

        return writeToFile("inputs-", ".zip", resource);
    }

    public File getTaskRunOutputsArchive(String uuid) {
        String url = baseUrl + "/task-runs/" + uuid + "/outputs.zip";
        byte[] resource = get(url, byte[].class).getBody();

        return writeToFile("outputs-", ".zip", resource);
    }

    public List<TaskRunParameterValue> postTaskRunOutputsArchive(String uuid, String secret, File outputs) {
        String url = baseUrl + "/task-runs/" + uuid + "/" + secret + "/outputs.zip";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("outputs", new FileSystemResource(outputs));
        String response = postData(url, body, String.class).getBody();

        return TaskTestsUtils.convertTo(response);
    }

    public List<TaskRunParameterValue> getTaskRunOutputs(String uuid) {
        String url = baseUrl + "/task-runs/" + uuid + "/outputs";
        String response = get(url, String.class).getBody();

        return TaskTestsUtils.convertTo(response);
    }
}
