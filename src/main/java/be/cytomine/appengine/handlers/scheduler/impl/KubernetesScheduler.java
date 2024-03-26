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

    @Value("${storage.host-path}")
    private String hostPath;

    @Override
    public Schedule schedule(Schedule schedule) throws SchedulingException {
        logger.info("Schedule: get Task parameters");

        Run run = schedule.getRun();
        Task task = run.getTask();

        String inputPath = hostPath + "/task-run-inputs-" + run.getId();
        String outputPath = hostPath + "/task-run-outputs-" + run.getId();

        String jobName = task.getName().replaceAll("[^a-zA-Z0-9]", "") + "-" + run.getId();
        String imageName = "localhost:5051/" + task.getImageName();

        logger.info("Schedule: create Task Job");
        Job job = new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                .withName(jobName)
                .withLabels(Collections.singletonMap("runId", run.getId().toString()))
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName(jobName)
                .withImage(imageName)
                .withImagePullPolicy("Never")
                .addNewVolumeMount()
                .withName("inputs")
                .withMountPath(task.getInputFolder())
                .endVolumeMount()
                .addNewVolumeMount()
                .withName("outputs")
                .withMountPath(task.getOutputFolder())
                .endVolumeMount()
                .endContainer()
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
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

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
