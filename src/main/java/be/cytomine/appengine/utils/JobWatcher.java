package be.cytomine.appengine.utils;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.states.TaskRunState;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

@Component
public class JobWatcher implements Watcher<Job> {

    private static final Logger logger = LoggerFactory.getLogger(JobWatcher.class);

    private RunRepository runRepository;

    public JobWatcher(RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    private TaskRunState processJobStatus(Job job, String runId) {
        logger.info("Cluster event: process job status " + job.getMetadata().getName());

        JobStatus status = job.getStatus();
        for (JobCondition condition : status.getConditions()) {
            switch (condition.getType()) {
                case "Complete":
                    return TaskRunState.RUNNING;
                case "Failed":
                default:
                    return TaskRunState.FAILED;
            }
        }

        return TaskRunState.RUNNING;
    }

    @Override
    public void eventReceived(Action action, Job job) {
        logger.info("Cluster event: job " + job.getMetadata().getName());

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
                run.setState(processJobStatus(job, runId));
                break;
            default:
                logger.info("Unrecognized event: " + action.name());
        }

        runRepository.saveAndFlush(run);
        logger.info("Running Task : updated Run state to " + run.getState());
    }

    @Override
    public void onClose(WatcherException cause) {
        logger.info("Watcher closed");
    }
}
