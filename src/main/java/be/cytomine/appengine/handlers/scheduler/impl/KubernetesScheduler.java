package be.cytomine.appengine.handlers.scheduler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.exceptions.SchedulingException;
import be.cytomine.appengine.handlers.SchedulerHandler;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Task;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesScheduler implements SchedulerHandler {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesScheduler.class);

    @Autowired
    private KubernetesClient kubernetesClient;

    @Value("${storage.base-path}")
    private String basePath;

    @Override
    public Schedule schedule(Schedule schedule) throws SchedulingException {
        logger.info("Schedule: get Task parameters");

        Run run = schedule.getRun();
        Task task = run.getTask();

        String inputPath = basePath + "/task-run-inputs-" + run.getId();
        String outputPath = basePath + "/task-run-outputs-" + run.getId();

        String jobName = task.getName().replaceAll("[^a-zA-Z0-9]", "") + "-" + run.getId();
        String imageName = "registry:5000/" + task.getImageName();

        logger.info("Schedule: create Task Job");
        Job job = new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                .withName(jobName)
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName(jobName)
                .withImage(imageName)
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
        kubernetesClient
                .batch()
                .v1()
                .jobs()
                .inNamespace("default")
                .resource(job)
                .create();

        return schedule;
    }

    @Override
    public void alive() throws SchedulingException {

    }

    @Override
    public void monitor() throws SchedulingException {

    }
}
