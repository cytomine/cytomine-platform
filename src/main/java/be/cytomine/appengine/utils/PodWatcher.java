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
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

@Component
public class PodWatcher implements Watcher<Pod> {

    private static final Logger logger = LoggerFactory.getLogger(PodWatcher.class);

    private RunRepository runRepository;

    public PodWatcher(RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    private TaskRunState processPodStatus(Pod pod) {
        logger.info("Pod Watcher: process status of pod " + pod.getMetadata().getName());

        switch (pod.getStatus().getPhase()) {
            case "Pending":
                return TaskRunState.PENDING;
            case "Running":
                return TaskRunState.RUNNING;
            case "Succeeded":
                return TaskRunState.FINISHED;
            case "Failed":
            case "Unknown":
            default:
                return TaskRunState.FAILED;
        }
    }

    @Override
    public void eventReceived(Action action, Pod pod) {
        logger.info("Pod Watcher: pod " + pod.getMetadata().getName());

        String runId = pod.getMetadata().getLabels().get("runId");
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
                run.setState(processPodStatus(pod));
                break;
            default:
                logger.info("Unrecognized event: " + action.name());
        }

        runRepository.saveAndFlush(run);
        logger.info("Pod Watcher: updated Run state to " + run.getState());
    }

    @Override
    public void onClose(WatcherException cause) {
        logger.info("Watcher closed");
    }
}
