package be.cytomine.appengine.utils;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.states.TaskRunState;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

@Component
public class JobWatcher implements Watcher<Job> {

    private static final Logger logger = LoggerFactory.getLogger(JobWatcher.class);

    private KubernetesClient kubernetesClient;

    private RunRepository runRepository;

    public JobWatcher(KubernetesClient kubernetesClient, RunRepository runRepository) {
        this.kubernetesClient = kubernetesClient;
        this.runRepository = runRepository;
    }

    private TaskRunState processJobStatus(Job job) {
        logger.info("Job Watcher: process status of job " + job.getMetadata().getName());

        // Check if the job has failed
        JobStatus status = job.getStatus();
        for (JobCondition condition : status.getConditions()) {
            if (condition.getType().equals("Failed")) {
                return TaskRunState.FAILED;
            }
        }

        // There is only one pod
        Pod pod = kubernetesClient
                .pods()
                .inNamespace("default")
                .withLabel("job-name", job.getMetadata().getName())
                .list()
                .getItems()
                .get(0);

        // Check the status of the pod
        switch (pod.getStatus().getPhase()) {
            case "Pending":
                return TaskRunState.PENDING;
            case "Running":
            case "Succeeded":
                return TaskRunState.RUNNING;
            case "Failed":
            case "Unknown":
            default:
                return TaskRunState.FAILED;
        }
    }

    @Override
    public void eventReceived(Action action, Job job) {
        logger.info("Job Watcher: job " + job.getMetadata().getName());

        String runId = job.getMetadata().getLabels().get("runId");
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            return;
        }

        Run run = runOptional.get();
        switch (action.name()) {
            case "ADDED":
                run.setState(TaskRunState.QUEUED);
                break;
            case "MODIFIED":
                run.setState(processJobStatus(job));
                break;
            default:
                logger.info("Unrecognized event: " + action.name());
        }

        runRepository.saveAndFlush(run);
        logger.info("Job Watcher: updated Run state to " + run.getState());
    }

    @Override
    public void onClose(WatcherException cause) {
        logger.info("Watcher closed");
    }
}
