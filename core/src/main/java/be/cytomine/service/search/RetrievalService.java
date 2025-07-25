package be.cytomine.service.search;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.dto.image.CropParameter;
import be.cytomine.dto.search.SearchResponse;
import be.cytomine.service.middleware.ImageServerService;

@Slf4j
@RequiredArgsConstructor
@Service
public class RetrievalService {

    public static final String CBIR_API_BASE_PATH = "/cbir";

    private final static String INDEX_NAME = "annotation";

    private final ApplicationProperties applicationProperties;

    private final ImageServerService imageServerService;

    private final RestTemplate restTemplate;

    public String getInternalCbirURL() {
        return applicationProperties.getInternalProxyURL() + CBIR_API_BASE_PATH;
    }

    public void createStorage(String projectId) {
        URI url = UriComponentsBuilder
            .fromHttpUrl(getInternalCbirURL())
            .path("/storages")
            .build()
            .toUri();

        Map<String, String> payload = new HashMap<>();
        payload.put("name", projectId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

        log.debug("Create cbir storage for project {}", projectId);
        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            requestEntity,
            String.class
        );

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to create storage for project {}", projectId);
            throw new RuntimeException("Failed to create storage: " + response.getBody());
        }
    }

    public void deleteStorage(String projectId) {
        URI url = UriComponentsBuilder
            .fromHttpUrl(getInternalCbirURL())
            .pathSegment("storages", projectId)
            .build()
            .toUri();

        log.debug("Create cbir storage for project {}", projectId);
        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            null,
            String.class
        );

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to delete storage for project {}", projectId);
            throw new RuntimeException("Failed to delete storage: " + response.getBody());
        }
    }

    private byte[] getImageAnnotation(AnnotationDomain annotation) {
        CropParameter parameters = new CropParameter();
        parameters.setComplete(true);
        parameters.setDraw(true);
        parameters.setFormat("png");
        parameters.setIncreaseArea(1.25);
        parameters.setLocation(annotation.getWktLocation());
        parameters.setMaxSize(256);

        try {
            ResponseEntity<byte[]> response = imageServerService.crop(annotation, parameters, null, null);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> createEntity(AnnotationDomain annotation) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        byte[] image = Objects.requireNonNull(getImageAnnotation(annotation));
        ByteArrayResource resource =  new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return annotation.getId().toString();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", resource);

        return new HttpEntity<>(body, headers);
    }

    public ResponseEntity<String> indexAnnotation(AnnotationDomain annotation) {
        String storageName = annotation.getProject().getId().toString();
        URI url = UriComponentsBuilder
            .fromHttpUrl(getInternalCbirURL())
            .path("/images")
            .queryParam("storage", storageName)
            .queryParam("index", INDEX_NAME)
            .build()
            .toUri();

        HttpEntity<MultiValueMap<String, Object>> requestEntity = createEntity(annotation);
        log.debug("Create index for annotation {}", annotation.getId());

        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
    }

    public ResponseEntity<String> deleteIndex(AnnotationDomain annotation) {
        URI url = UriComponentsBuilder
            .fromHttpUrl(getInternalCbirURL())
            .pathSegment("images", annotation.getId().toString())
            .queryParam("storage", annotation.getProject().getId())
            .queryParam("index", INDEX_NAME)
            .build()
            .toUri();

        log.debug("Delete index for annotation {}", annotation.getId());
        return restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            null,
            String.class
        );
    }

    private List<List<Object>> processSimilarities(List<List<Object>> similarities, double maxDistance) {
        List<List<Object>> percentages = new ArrayList<>();

        for (List<Object> entry : similarities) {
            String item = (String) entry.get(0);
            Double distance = (Double) entry.get(1);
            Double percentage = (1 - (distance / maxDistance)) * 100;

            percentages.add(List.of(item, percentage));
        }

        return percentages;
    }

    public ResponseEntity<SearchResponse> retrieveSimilarImages(AnnotationDomain annotation, Long nrt_neigh) {
        String url = UriComponentsBuilder
            .fromHttpUrl(getInternalCbirURL())
            .path("/search")
            .queryParam("storage", annotation.getProject().getId())
            .queryParam("index", INDEX_NAME)
            .queryParam("nrt_neigh", nrt_neigh + 1)
            .toUriString();

        HttpEntity<MultiValueMap<String, Object>> requestEntity = createEntity(annotation);

        ResponseEntity<SearchResponse> response = this.restTemplate.exchange(
            url,
            HttpMethod.POST,
            requestEntity,
            SearchResponse.class
        );
        log.debug("Receiving response {}", response);

        SearchResponse searchResponse = response.getBody();
        if (searchResponse == null) {
            return response;
        }

        searchResponse.getSimilarities().remove(0);

        double maxDistance = searchResponse
            .getSimilarities()
            .stream()
            .mapToDouble(d -> (Double) d.get(1))
            .max()
            .orElse(1.0);
        searchResponse.setSimilarities(processSimilarities(searchResponse.getSimilarities(), maxDistance));

        return ResponseEntity.ok(searchResponse);
    }
}