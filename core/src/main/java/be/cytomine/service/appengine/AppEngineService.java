package be.cytomine.service.appengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import be.cytomine.controller.JsonResponseEntity;

@Slf4j
@Service
public class AppEngineService {

    @Value("${application.internalProxyURL}")
    private String internalProxyUrl;

    @Value("${application.appEngine.apiBasePath}")
    private String apiBasePath;

    private String buildFullUrl(String uri) {
        return internalProxyUrl + apiBasePath + uri;
    }

    public ResponseEntity<String> get(String uri) {
        try {
            return new RestTemplate().getForEntity(buildFullUrl(uri), String.class);
        } catch (HttpClientErrorException | HttpServerErrorException.InternalServerError e) {
            return JsonResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    public ResponseEntity<byte[]> getByte(String uri) {
        try {
            return new RestTemplate().getForEntity(buildFullUrl(uri), byte[].class);
        } catch (HttpClientErrorException | HttpServerErrorException.InternalServerError e) {
            return JsonResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
        }
    }

    public File getStreamedFile(String uri) {
        Path filePath = Paths.get("downloaded_" + System.currentTimeMillis() + ".tmp");
        File targetFile = filePath.toFile();

        new RestTemplate().execute(
            buildFullUrl(uri),
            HttpMethod.GET,
            null,
            response -> {
                try (InputStream in = response.getBody();
                        OutputStream out = new FileOutputStream(targetFile)) {
                    StreamUtils.copy(in, out);
                    return null;
                }
            }
        );

        return targetFile;
    }

    public <B> ResponseEntity<String> sendWithBody(HttpMethod method, String uri, B body, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        HttpEntity<B> request = new HttpEntity<>(body, headers);

        try {
            if (method.matches("POST")) {
                return new RestTemplate().postForEntity(buildFullUrl(uri), request, String.class);
            } else if (method.matches("PUT")) {
                return new RestTemplate().exchange(buildFullUrl(uri), HttpMethod.PUT, request, String.class);
            } else {
                throw new NotImplementedException("sendWithBody not implemented with method than {POST, PUT}");
            }
        } catch (HttpClientErrorException | HttpServerErrorException.InternalServerError e) {
            return JsonResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    public <B> ResponseEntity<String> post(String uri, B body, MediaType contentType) {
        return sendWithBody(HttpMethod.POST, uri, body, contentType);
    }

    public <B> ResponseEntity<String> put(String uri, B body, MediaType contentType) {
        return sendWithBody(HttpMethod.PUT, uri, body, contentType);
    }

    public <B> ResponseEntity<String> putWithParams(String uri, B body, MediaType contentType, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(uri);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        String finalUrl = builder.toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);

        HttpEntity<B> requestEntity = new HttpEntity<>(body, headers);

        return new RestTemplate().exchange(buildFullUrl(finalUrl), HttpMethod.PUT, requestEntity, String.class);
    }
}
