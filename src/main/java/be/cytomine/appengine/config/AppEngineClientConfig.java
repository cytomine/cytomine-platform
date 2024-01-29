package be.cytomine.appengine.config;

import be.cytomine.appengine.openapi.api.DefaultApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppEngineClientConfig {

    // this will be used in the tests to test the api against the OpenAPI specs
    @Bean
    public DefaultApi appEngineAPIClient() {
        return new DefaultApi();
    }
}
