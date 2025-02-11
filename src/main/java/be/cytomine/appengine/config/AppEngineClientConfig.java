package be.cytomine.appengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import be.cytomine.appengine.openapi.api.DefaultApi;

@Configuration
public class AppEngineClientConfig {

    // this will be used in the tests to test the api against the OpenAPI specs
    @Bean
    public DefaultApi appEngineApiClient() {
        return new DefaultApi();
    }
}
