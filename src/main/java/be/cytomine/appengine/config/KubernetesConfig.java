package be.cytomine.appengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

@Configuration
public class KubernetesConfig {

    @Value("${scheduler.master-url}")
    private String masterUrl;

    @Value("${scheduler.username}")
    private String username;

    @Value("${scheduler.oauth-token}")
    private String OAuthToken;

    @Bean
    public KubernetesClient kubernetesClient() {
        Config configurations = new ConfigBuilder()
                .withMasterUrl(masterUrl)
                .withUsername(username)
                .withOauthToken(OAuthToken)
                .withTrustCerts(true)
                .build();

        return new KubernetesClientBuilder()
                .withConfig(configurations)
                .build();
    }
}
