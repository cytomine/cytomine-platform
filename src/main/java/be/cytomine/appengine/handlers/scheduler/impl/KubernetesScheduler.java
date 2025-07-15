package be.cytomine.appengine.handlers.scheduler.impl;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.exceptions.SchedulingException;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.handlers.scheduler.impl.utils.PodInformer;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.states.TaskRunState;

@Slf4j
public class KubernetesScheduler implements SchedulerHandler {

    @Autowired
    private Environment environment;

    @Autowired
    private KubernetesClient kubernetesClient;

    @Autowired
    private RunRepository runRepository;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    @Value("${registry-client.host}")
    private String registryHost;

    @Value("${registry-client.port}")
    private String registryPort;

    @Value("${scheduler.helper-containers-resources.ram}")
    private String helperContainerRam;

    @Value("${scheduler.helper-containers-resources.cpu}")
    private String helperContainerCpu;

    @Value("${storage.base-path}")
    private String storageBasePath;

    @Value("${scheduler.run-mode}")
    private String runMode;

    private PodInformer podInformer;

    private String baseUrl;

    private String baseInputPath;

    private String baseOutputPath;

    private boolean isInDocker() {
        return new File("/.dockerenv").exists();
    }

    private String getHostAddress() throws SchedulingException {
        if (!isInDocker()) {
            return "172.17.0.1";
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new SchedulingException("Failed to get the hostname the app engine");
        }
    }

    private String getRegistryAddress() throws SchedulingException {
        try {
            InetAddress address = InetAddress.getByName(registryHost);
            return address.getHostAddress() + ":" + registryPort;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new SchedulingException("Failed to get the hostname of the registry");
        }
    }

    @PostConstruct
    private void initUrl() throws SchedulingException {
        String port = environment.getProperty("server.port");
        String hostAddress = "http://" + getHostAddress();

        this.baseUrl = hostAddress + ":" + port + apiPrefix + apiVersion + "/task-runs/";
        String basePath = "";
        if (runMode.equalsIgnoreCase("local")) {
            basePath =  storageBasePath;
        }
        if (runMode.equalsIgnoreCase("cluster")) {
            basePath = "/tmp/app-engine";
        }
        this.baseInputPath = basePath + "/task-run-inputs-";
        this.baseOutputPath = basePath + "/task-run-outputs-";
    }

    @Override
    public Schedule schedule(Schedule schedule) throws SchedulingException {
        log.info("Schedule: get Task parameters");

        Run run = schedule.getRun();
        String runId = run.getId().toString();
        Map<String, String> labels = new HashMap<>();
        labels.put("runId", runId);

        Task task = run.getTask();
        String podName = task.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "") + "-" + runId;
        String imageName = getRegistryAddress() + "/" + task.getImageName();
        String runSecret = String.valueOf(run.getSecret());

        log.info("Schedule: create task pod...");

        // Define helper container resources
        ResourceRequirementsBuilder helperContainersResourcesBuilder =
            new ResourceRequirements()
            .toBuilder()
            .addToRequests("cpu", new Quantity(helperContainerCpu))
            .addToRequests("memory", new Quantity(helperContainerRam))
            .addToLimits("cpu", new Quantity(helperContainerCpu))
            .addToLimits("memory", new Quantity(helperContainerRam));

        ResourceRequirements helperContainersResources = helperContainersResourcesBuilder.build();

        // Define task resources for the task
        ResourceRequirementsBuilder taskResourcesBuilder =
            new ResourceRequirements()
            .toBuilder()
            .addToRequests("cpu", new Quantity(Integer.toString(task.getCpus())))
            .addToRequests("memory", new Quantity(task.getRam()))
            .addToLimits("cpu", new Quantity(Integer.toString(task.getCpus())))
            .addToLimits("memory", new Quantity(task.getRam()));

        if (task.getGpus() > 0) {
            taskResourcesBuilder =
                taskResourcesBuilder
                    .addToRequests("nvidia.com/gpu", new Quantity(Integer.toString(task.getGpus())))
                    .addToLimits("nvidia.com/gpu", new Quantity(Integer.toString(task.getGpus())));
        }

        ResourceRequirements taskResources = taskResourcesBuilder.build();

        String url = baseUrl + runId;
        String and = " && ";

        String permissions = "chmod -R 777 " + task.getInputFolder() + " " + task.getOutputFolder();
        Container permissionContainer = new ContainerBuilder()
            .withName("permissions")
            .withImage("cytomineuliege/alpine-task-utils:latest")
            .withImagePullPolicy("IfNotPresent")
            .withCommand("/bin/sh", "-c", permissions)

            .withResources(helperContainersResources)

            .addNewVolumeMount()
            .withName("inputs")
            .withMountPath(task.getInputFolder())
            .endVolumeMount()
            .addNewVolumeMount()
            .withName("outputs")
            .withMountPath(task.getOutputFolder())
            .endVolumeMount()

            .build();

        String fetchInputs = "curl -L -o inputs.zip " + url + "/inputs.zip";
        String unzipInputs = "time unzip -o inputs.zip -d " + task.getInputFolder();

        Container inputContainer = new ContainerBuilder()
            .withName("inputs-provisioning")
            .withImage("cytomineuliege/alpine-task-utils:latest")
            .withImagePullPolicy("IfNotPresent")
            .withCommand("/bin/sh", "-c", fetchInputs + and + unzipInputs)

            // request and limit helper container resources
            .withResources(helperContainersResources)

            // Mount volume for inputs provisioning
            .addNewVolumeMount()
            .withName("inputs")
            .withMountPath(task.getInputFolder())
            .endVolumeMount()

            .build();

        Container taskContainer = new ContainerBuilder()
            .withName("task")
            .withImage(imageName)
            .withImagePullPolicy("IfNotPresent")

            // request and limit task resources
            .withResources(taskResources)

            // Mount volumes for inputs and outputs
            .addNewVolumeMount()
            .withName("inputs")
            .withMountPath(task.getInputFolder())
            .endVolumeMount()
            .addNewVolumeMount()
            .withName("outputs")
            .withMountPath(task.getOutputFolder())
            .endVolumeMount()

            .build();

        String sendOutputs = "curl -X POST -F 'outputs=@outputs.zip' ";
        sendOutputs += url + "/" + runSecret + "/outputs.zip";
        String zipOutputs = "cd " + task.getOutputFolder() + and + " zip -r outputs.zip .";
        String wait = "export TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token); ";
        wait += "while ! curl -vk -H \"Authorization: Bearer $TOKEN\" ";
        wait += "https://${KUBERNETES_SERVICE_HOST}:${KUBERNETES_SERVICE_PORT_HTTPS}/api/v1/namespaces/default/pods/${POD_NAME}/status ";
        wait += "| jq '.status | .containerStatuses[] | select(.name == \"task\") | .state ";
        wait += "| keys[0]' | grep -q -F \"terminated\"; do sleep 2; done";

        String clusterOutputCommand = wait + and + zipOutputs + and + sendOutputs;
        String updateAPIRequestJson = " '{ \"desired\" : \"FINISHED\" }' ";
        String requestToUpdateRunStateToFinished = "curl -X POST -H 'Content-Type: application/json' -d";
        requestToUpdateRunStateToFinished += updateAPIRequestJson;
        requestToUpdateRunStateToFinished += url + "/" + "state-actions";
        String localOutputCommand = wait + and + requestToUpdateRunStateToFinished;

        String command = "";
        if (runMode.equalsIgnoreCase("local")) {
            command = localOutputCommand;
        }
        if (runMode.equalsIgnoreCase("cluster")) {
            command = clusterOutputCommand;
        }

        Container outputContainer = new ContainerBuilder()
            .withName("outputs-sending")
            .withImage("cytomineuliege/alpine-task-utils:latest")
            .withImagePullPolicy("IfNotPresent")
            .withCommand("/bin/sh", "-c", command)

            // request and limit helper container resources
            .withResources(helperContainersResources)

            .addNewVolumeMount()
            .withName("outputs")
            .withMountPath(task.getOutputFolder())
            .endVolumeMount()

            .withEnv(new EnvVarBuilder()
            .withName("POD_NAME")
            .withNewValueFrom()
            .withNewFieldRef()
            .withFieldPath("metadata.name")
            .endFieldRef()
            .endValueFrom()
            .build())

            .build();

        boolean isClusterMode = this.runMode.equalsIgnoreCase("cluster");
        // Defining the pod image to run

        PodBuilder podBuilder = new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withLabels(labels)
            .endMetadata()
            .withNewSpec()
            .withHostNetwork(true)
            .addNewInitContainerLike(permissionContainer)
            .and()
            .withRestartPolicy("Never")
            .endSpec();

        // After .endSpec(), modify the pod to add inputContainer and outputContainer conditionally,
        // taskContainer unconditionally, and inputs/outputs volumes conditionally
        Pod pod = podBuilder.build();
        PodBuilder newPodBuilder = new PodBuilder(pod);
        List<Container> initContainers = new ArrayList<>(pod.getSpec().getInitContainers());
        List<Container> containers = new ArrayList<>(pod.getSpec().getContainers());
        List<Volume> volumes = new ArrayList<>(pod.getSpec().getVolumes());

        // Add inputContainer conditionally
        if (isClusterMode) {
            initContainers.add(inputContainer);
        }

        // Add taskContainer unconditionally
        containers.add(taskContainer);
        containers.add(outputContainer);

        // Add inputs and outputs volumes conditionally
        volumes.add(new VolumeBuilder()
             .withName("inputs")
             .withHostPath(
                 new HostPathVolumeSourceBuilder().withPath(baseInputPath + runId).build())
             .build());
        volumes.add(new VolumeBuilder()
             .withName("outputs")
             .withHostPath(
                 new HostPathVolumeSourceBuilder().withPath(baseOutputPath + runId).build())
             .build());

        newPodBuilder = newPodBuilder.editOrNewSpec()
            .withInitContainers(initContainers)
            .withContainers(containers)
            .withVolumes(volumes)
            .endSpec();

        podBuilder = newPodBuilder;

        log.info("Schedule: Task Pod scheduled to run on the cluster");
        try {
            kubernetesClient
                .pods()
                .inNamespace("default")
                .resource(podBuilder.build())
                .create();
        } catch (KubernetesClientException e) {
            e.printStackTrace();
            throw new SchedulingException("Task Pod failed to be scheduled on the cluster");
        }

        run.setState(TaskRunState.QUEUED);
        runRepository.saveAndFlush(run);
        log.info("Schedule: Task Pod queued for execution on the cluster");

        return schedule;
    }

    @Override
    public void alive() throws SchedulingException {
        log.info("Alive: check if the scheduler is up and running");

        try {
            kubernetesClient
                .pods()
                .inNamespace("default")
                .list();
        } catch (KubernetesClientException e) {
            throw new SchedulingException("Scheduler is not alive");
        }
    }

    @Override
    @PostConstruct
    public void monitor() throws SchedulingException {
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            log.info("Monitor: disabled in test mode");
            return;
        }

        log.info("Monitor: add informer to the cluster");
        kubernetesClient
            .pods()
            .inNamespace("default")
            .inform(podInformer)
            .run();
        log.info("Monitor: informer added");
    }
}
