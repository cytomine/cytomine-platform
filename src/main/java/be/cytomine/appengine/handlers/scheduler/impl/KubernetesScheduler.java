package be.cytomine.appengine.handlers.scheduler.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.exceptions.SchedulingException;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.handlers.scheduler.impl.utils.PodEvent;
import be.cytomine.appengine.handlers.scheduler.impl.utils.PodWatcher;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jakarta.annotation.PostConstruct;

public class KubernetesScheduler implements SchedulerHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesScheduler.class);

    @Autowired
    private Environment environment;

    @Autowired
    private KubernetesClient kubernetesClient;

    @Autowired
    private PodWatcher podWatcher;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    @Value("${registry-client.host}")
    private String registryHost;

    @Value("${registry-client.port}")
    private String registryPort;

    private String baseUrl;

    private String baseInputPath;

    private String baseOutputPath;

    private String getHostAddress() throws SchedulingException {
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
        String hostAddress = getHostAddress();

        this.baseUrl = hostAddress + ":" + port + "/app-engine/v1/task-runs/";
        this.baseInputPath = "/tmp/app-engine/task-run-inputs-";
        this.baseOutputPath = "/tmp/app-engine/task-run-outputs-";
    }

    @EventListener
    private void schedulePostTask(PodEvent event) throws SchedulingException {
        Map<String, String> labels = event.getLabels();
        String runId = labels.get("runId");
        String podName = "outputs-sending-" + runId;

        // Avoid creating the post task on every watch event
        Pod pod = kubernetesClient
                .pods()
                .inNamespace("default")
                .withName(podName)
                .get();
        if (pod != null) {
            return;
        }

        logger.info("Schedule: create post task pod...");

        String url = baseUrl + runId;
        String outputFolder = labels.get("outputFolder");

        // Post container commands
        String installDeps = "apk --no-cache add curl zip";
        String sendOutputs = "curl -X POST -F 'outputs=@outputs.zip' " + url + "/outputs.zip";
        String zipOutputs = "zip -rj outputs.zip " + outputFolder;
        String and = " && ";

        Map<String, String> podLabels = new HashMap<>() {
            {
                put("postTask", "true");
                put("runId", runId);
            }
        };
        PodBuilder podBuilder = new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .withLabels(podLabels)
                .endMetadata();

        // Defining the pod image to run
        podBuilder
                .withNewSpec()

                // Add post-container for sending outputs
                .addNewContainer()
                .withName(podName)
                .withImage("alpine:latest")
                .withImagePullPolicy("IfNotPresent")
                .withCommand("/bin/sh", "-c", installDeps + and + zipOutputs + and + sendOutputs)

                // Mount volume for outputs
                .addNewVolumeMount()
                .withName("outputs")
                .withMountPath(outputFolder)
                .endVolumeMount()

                .endContainer()

                // Mount volume from the scheduler file system
                .addToVolumes(new VolumeBuilder()
                        .withName("outputs")
                        .withHostPath(new HostPathVolumeSourceBuilder()
                                .withPath(baseOutputPath + runId)
                                .build())
                        .build())

                // Never restart the pod
                .withRestartPolicy("Never")

                .endSpec();

        logger.info("Schedule: Post task Pod scheduled to run on the cluster");
        try {
            kubernetesClient
                    .pods()
                    .inNamespace("default")
                    .resource(podBuilder.build())
                    .create();
        } catch (KubernetesClientException e) {
            e.printStackTrace();
            throw new SchedulingException("Post task failed to be scheduled on the cluster");
        }
        logger.info("Schedule: Post task Pod queued for execution on the cluster");
    }

    @Override
    public Schedule schedule(Schedule schedule) throws SchedulingException {
        logger.info("Schedule: get Task parameters");

        Run run = schedule.getRun();
        String runId = run.getId().toString();
        Task task = run.getTask();

        String podName = task.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "") + "-" + runId;
        String imageName = getRegistryAddress() + "/" + task.getImageName();

        // Pre container commands
        String url = baseUrl + runId;
        String installDeps = "apk --no-cache add curl zip";
        String fetchInputs = "curl -L -o inputs.zip " + url + "/inputs.zip";
        String unzipInputs = "unzip -o inputs.zip -d " + task.getInputFolder();
        String and = " && ";

        Map<String, String> labels = new HashMap<>() {
            {
                put("postTask", "false");
                put("runId", runId);
            }
        };

        logger.info("Schedule: create task pod...");
        PodBuilder podBuilder = new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .withLabels(labels)
                .endMetadata();

        // Defining the pod image to run
        podBuilder
                .withNewSpec()

                // Add pre-container for inputs provisioning
                .addNewInitContainer()
                .withName("inputs-provisioning-" + runId)
                .withImage("alpine:latest")
                .withImagePullPolicy("IfNotPresent")
                .withCommand("/bin/sh", "-c", installDeps + and + fetchInputs + and + unzipInputs)

                // Mount volume for inputs provisioning
                .addNewVolumeMount()
                .withName("inputs")
                .withMountPath(task.getInputFolder())
                .endVolumeMount()

                .endInitContainer()

                // Add Task container
                .addNewContainer()
                .withName(podName)
                .withImage(imageName)
                .withImagePullPolicy("IfNotPresent")

                // Mount volumes for inputs and outputs
                .addNewVolumeMount()
                .withName("inputs")
                .withMountPath(task.getInputFolder())
                .endVolumeMount()

                .addNewVolumeMount()
                .withName("outputs")
                .withMountPath(task.getOutputFolder())
                .endVolumeMount()

                .endContainer()

                // Mount volumes from the scheduler file system
                .addToVolumes(new VolumeBuilder()
                        .withName("inputs")
                        .withHostPath(new HostPathVolumeSourceBuilder()
                                .withPath(baseInputPath + runId)
                                .build())
                        .build())
                .addToVolumes(new VolumeBuilder()
                        .withName("outputs")
                        .withHostPath(new HostPathVolumeSourceBuilder()
                                .withPath(baseOutputPath + runId)
                                .build())
                        .build())

                // Never restart the pod
                .withRestartPolicy("Never")

                .endSpec();

        logger.info("Schedule: Task Pod scheduled to run on the cluster");
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
        logger.info("Schedule: Task Pod queued for execution on the cluster");

        return schedule;
    }

    @Override
    public void alive() throws SchedulingException {
        logger.info("Alive: check if the scheduler is up and running");

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
        logger.info("Monitor: add watcher to the cluster");

        try {
            kubernetesClient
                    .pods()
                    .inNamespace("default")
                    .watch(podWatcher);
        } catch (KubernetesClientException e) {
            throw new SchedulingException("Failed to add watcher to the cluster");
        }
    }
}
