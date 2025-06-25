package be.cytomine.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class ResourceConfiguration {
    @Bean
    @Qualifier("resourceLoader")
    public ResourceLoader resourceLoader() {
        return new DefaultResourceLoader();
    }
}
