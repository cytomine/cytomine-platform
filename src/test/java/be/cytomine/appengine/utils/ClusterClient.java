package be.cytomine.appengine.utils;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Component;

@Component
public class ClusterClient {

    private final KubernetesClient kubernetesClient;

    public ClusterClient(KubernetesClient client) {
        kubernetesClient = client;
    }

    public Map<String, String> getAllocatedResources(String uuid) {
        Pod pod = kubernetesClient.pods().inNamespace("default").withName(uuid).get();
        
        Container task = pod.getSpec()
            .getContainers()
            .stream()
            .filter(container -> container.getName().equals("task"))
            .findFirst()
            .orElse(null);

        ResourceRequirements allocatedResources = task.getResources();
        if (allocatedResources == null) {
            return new HashMap<>();
        }

        Map<String, String> resources = new HashMap<>();
        Map<String, Quantity> limits = allocatedResources.getLimits();
        resources.put("ram", limits.getOrDefault("memory", new Quantity("0")).toString());
        resources.put("cpu", limits.getOrDefault("cpu", new Quantity("0")).getAmount());
        resources.put("gpu", limits.getOrDefault("nvidia.com/gpu", new Quantity("0")).getAmount());

        return resources;
    }
}
