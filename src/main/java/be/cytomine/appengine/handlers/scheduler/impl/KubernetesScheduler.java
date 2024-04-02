package be.cytomine.appengine.handlers.scheduler.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.exceptions.SchedulingException;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import be.cytomine.appengine.utils.JobWatcher;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jakarta.annotation.PostConstruct;

public class KubernetesScheduler implements SchedulerHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesScheduler.class);

    @Autowired
    private KubernetesClient kubernetesClient;

    @Autowired
    private JobWatcher jobWatcher;

    @Value("${app-engine.api_prefix}")
    private String apiPrefix;

    @Value("${app-engine.api_version}")
    private String apiVersion;

    @Value("${HOSTNAME}")
    private String hostname;

    @Autowired
    private Environment environment;

    @Override
    public Schedule schedule(Schedule schedule) throws SchedulingException {
        logger.info("Schedule: get Task parameters");

        String port = environment.getProperty("server.port");
        String hostAddress = null;
        try {
            InetAddress address = InetAddress.getByName(hostname);
            hostAddress = address.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new SchedulingException("Failed to get the hostname");
        }

        Run run = schedule.getRun();
        String runId = run.getId().toString();
        Task task = run.getTask();

        String inputPath = "/tmp/app-engine/task-run-inputs-" + runId;
        String outputPath = "/tmp/app-engine/task-run-outputs-" + runId;

        String jobName = task.getName().replaceAll("[^a-zA-Z0-9]", "") + "-" + runId;
        String imageName = "localhost:5051/" + task.getImageName();

        // Pre and post job commands
        String url = "http://" + hostAddress + ":" + port + "/app-engine/v1/task-runs/" + runId;
        String installDeps = "apk --no-cache add curl zip";
        String fetchInputs = "curl -L -o inputs.zip " + url + "/inputs.zip";
        String unzipInputs = "unzip -o inputs.zip -d " + task.getInputFolder();
        String sendOutputs = "curl -X POST -F 'outputs=@outputs.zip' " + url + "/outputs.zip";
        String zipOutputs = "zip -rj outputs.zip " + task.getOutputFolder();
        String and = " && ";

        logger.info("Schedule: create task job...");
        JobBuilder jobBuilder = new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                .withName(jobName)
                .withLabels(Collections.singletonMap("runId", run.getId().toString()))
                .endMetadata();

        // Defining the job image to run
        jobBuilder
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()

                // Add pre-job container for inputs provisioning
                .addNewInitContainer()
                .withName("pre-job-" + runId)
                .withImage("alpine:latest")
                .withImagePullPolicy("Always")
                .withCommand("/bin/sh", "-c", installDeps + and + fetchInputs + and + unzipInputs)

                // Mount volumes for inputs provisioning
                .addNewVolumeMount()
                .withName("inputs")
                .withMountPath(task.getInputFolder())
                .endVolumeMount()

                .endInitContainer()

                // Add main job container
                .addNewContainer()
                .withName(jobName)
                .withImage(imageName)
                .withImagePullPolicy("Always")

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

                // Add post job container for sending outputs
                .addNewContainer()
                .withName("post-job-" + runId)
                .withImage("alpine:latest")
                .withCommand("/bin/sh", "-c", installDeps + and + zipOutputs + and + sendOutputs)

                // Mount volume for outputs
                .addNewVolumeMount()
                .withName("outputs")
                .withMountPath(task.getOutputFolder())
                .endVolumeMount()

                .endContainer()

                // Mount volumes from the scheduler file system
                .addToVolumes(new VolumeBuilder()
                        .withName("inputs")
                        .withHostPath(new HostPathVolumeSourceBuilder()
                                .withPath(inputPath)
                                .build())
                        .build())
                .addToVolumes(new VolumeBuilder()
                        .withName("outputs")
                        .withHostPath(new HostPathVolumeSourceBuilder()
                                .withPath(outputPath)
                                .build())
                        .build())

                // Never restart the job
                .withRestartPolicy("Never")

                .endSpec()
                .endTemplate()
                .endSpec();

        Job job = jobBuilder.build();

        logger.info("Schedule: Task Job scheduled to run on the cluster");
        try {
            kubernetesClient
                    .batch()
                    .v1()
                    .jobs()
                    .inNamespace("default")
                    .resource(job)
                    .create();
        } catch (KubernetesClientException e) {
            e.printStackTrace();
            throw new SchedulingException("Task Job failed to be scheduled on the cluster");
        }
        logger.info("Schedule: Task Job queued for execution on the cluster");

        return schedule;
    }

    @Override
    public void alive() throws SchedulingException {
    }

    @Override
    @PostConstruct
    public void monitor() throws SchedulingException {
        logger.info("Monitor: add watcher to the cluster");

        try {
            kubernetesClient
                    .batch()
                    .v1()
                    .jobs()
                    .inNamespace("default")
                    .watch(jobWatcher);
        } catch (KubernetesClientException e) {
            throw new SchedulingException("Failed to add watcher to the cluster");
        }
    }
}
