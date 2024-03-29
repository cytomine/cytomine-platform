package be.cytomine.appengine.handlers.scheduler.impl;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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

    @Override
    public Schedule schedule(Schedule schedule) throws SchedulingException {
        logger.info("Schedule: get Task parameters");

        Run run = schedule.getRun();
        String runId = run.getId().toString();
        Task task = run.getTask();

        String inputPath = "/data/app-engine/task-run-inputs-" + runId;
        String outputPath = "/data/app-engine/task-run-outputs-" + runId;

        String jobName = task.getName().replaceAll("[^a-zA-Z0-9]", "") + "-" + runId;
        String imageName = "localhost:5051/" + task.getImageName();

        logger.info("Schedule: create Task Job");
        // Defining the job metadata
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

                // Add pre job container for inputs provisioning
                .addNewContainer()
                .withName("pre-job-" + runId)
                .withImage("localhost:5051/pre-job:0.1.0")
                .withArgs(runId, "172.19.0.5", "8080")
                .withImagePullPolicy("IfNotPresent")

                // Mount volume for inputs
                .addNewVolumeMount()
                .withName("inputs")
                .withMountPath(task.getInputFolder())
                .endVolumeMount()

                .endContainer()

                // Add main job container
                .addNewContainer()
                .withName(jobName)
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

                // Add post job container for sending outputs
                .addNewContainer()
                .withName("post-job-" + runId)
                .withImage("localhost:5051/post-job:0.1.0")
                .withArgs(runId, "172.19.0.5", "8080")
                .withImagePullPolicy("IfNotPresent")

                // Mount volume for outputs
                .addNewVolumeMount()
                .withName("outputs")
                .withMountPath(task.getOutputFolder())
                .endVolumeMount()

                .endContainer()

                // Mounts volumes from the scheduler file system
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
