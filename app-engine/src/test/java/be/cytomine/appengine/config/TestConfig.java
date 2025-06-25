package be.cytomine.appengine.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import be.cytomine.appengine.utils.ApiClient;
import be.cytomine.appengine.utils.ClusterClient;

@TestConfiguration
public class TestConfig {
    @Bean
    public ApiClient apiClient() {
        return new ApiClient();
    }

    @Bean
    public ClusterClient clusterClient(KubernetesClient kubernetesClient) {
        return new ClusterClient(kubernetesClient);
    }
}
