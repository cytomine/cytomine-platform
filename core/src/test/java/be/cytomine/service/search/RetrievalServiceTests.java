package be.cytomine.service.search;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.controller.ontology.UserAnnotationResourceTests;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.dto.search.SearchResponse;

import static be.cytomine.service.middleware.ImageServerService.IMS_API_BASE_PATH;
import static be.cytomine.service.search.RetrievalService.CBIR_API_BASE_PATH;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = CytomineCoreApplication.class)
public class RetrievalServiceTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private RetrievalService retrievalService;

    private static WireMockServer wireMockServer;

    private static void setupStub() {
        /* Simulate call to PIMS */
        wireMockServer.stubFor(WireMock.post(urlPathMatching(IMS_API_BASE_PATH + "/image/.*/annotation/drawing"))
            .withRequestBody(WireMock.matching(".*"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withBody(UUID.randomUUID().toString().getBytes())
            )
        );
    }

    @BeforeAll
    public static void beforeAll() {
        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();

        setupStub();
    }

    @AfterAll
    public static void afterAll() {
        wireMockServer.stop();
    }

    @Test
    public void index_annotation_with_success() throws ParseException {
        UserAnnotation annotation = UserAnnotationResourceTests.given_a_user_annotation_with_valid_image_server(builder);

        /* Simulate call to CBIR */
        String expectedUrlPath = CBIR_API_BASE_PATH + "/images";
        String expectedResponseBody = "{ \"ids\": [" + annotation.getId() + "]";
        expectedResponseBody += ", \"storage\": " + annotation.getProject().getId().toString();
        expectedResponseBody += ", \"index\": \"annotation\" }";

        wireMockServer.stubFor(WireMock.post(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(expectedResponseBody)
            )
        );

        /* Test index annotation method */
        ResponseEntity<String> response = retrievalService.indexAnnotation(annotation);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponseBody, response.getBody());

        wireMockServer.verify(WireMock.postRequestedFor(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation")));
    }

    @Test
    public void delete_index_with_success() {
        UserAnnotation annotation = builder.given_a_user_annotation();

        /* Simulate call to CBIR */
        String expectedUrlPath = CBIR_API_BASE_PATH + "/images/" + annotation.getId();
        String expectedResponseBody = "{ \"id\": " + annotation.getId();
        expectedResponseBody += ", \"storage\": " + annotation.getProject().getId().toString();
        expectedResponseBody += ", \"index\": \"annotation\" }";

        wireMockServer.stubFor(WireMock.delete(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(expectedResponseBody)
            )
        );

        /* Test delete index method */
        ResponseEntity<String> response = retrievalService.deleteIndex(annotation);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponseBody, response.getBody());

        wireMockServer.verify(WireMock.deleteRequestedFor(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation")));
    }

    @Test
    public void search_similar_images_with_success() throws JsonProcessingException, ParseException {
        UserAnnotation annotation = UserAnnotationResourceTests.given_a_user_annotation_with_valid_image_server(builder);

        /* Simulate call to CBIR */
        ObjectMapper objectMapper = new ObjectMapper();
        String expectedUrlPath = CBIR_API_BASE_PATH + "/search";
        SearchResponse expectedResponse = new SearchResponse(
            annotation.getId().toString(),
            "annotation",
            List.of(annotation.getProject().getId().toString()),
            List.of(
                List.of(annotation.getId().toString(), 0.0),
                List.of("1", 123.0),
                List.of("2", 456.0)
            )
        );

        wireMockServer.stubFor(WireMock.post(urlPathEqualTo(expectedUrlPath))
            .withQueryParam("storage", WireMock.equalTo(annotation.getProject().getId().toString()))
            .withQueryParam("index", WireMock.equalTo("annotation"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(expectedResponse))
            )
        );

        /* Test retrieve similar images method */
        Long neighbours = 2L;
        ResponseEntity<SearchResponse> response = retrievalService.retrieveSimilarImages(
            annotation,
            neighbours
        );

        expectedResponse.setSimilarities(
            List.of(List.of("1", 73.02631578947368), List.of("2", 0.0))
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }
}
