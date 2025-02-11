package be.cytomine.appengine.handlers.scheduler.impl.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.states.TaskRunState;

@Slf4j
@AllArgsConstructor
@Component
public class PodInformer implements ResourceEventHandler<Pod> {

    private static final Map<String, TaskRunState> STATUS = new HashMap<String, TaskRunState>() {{
        put("Running", TaskRunState.RUNNING);
        put("Succeeded", TaskRunState.RUNNING);
        put("Failed", TaskRunState.FAILED);
        put("Unknown", TaskRunState.FAILED);
    }};

    private static final Set<TaskRunState> FINAL_STATES = Set.of(TaskRunState.FAILED, TaskRunState.FINISHED);

    private final RunRepository runRepository;

    private Run getRun(Pod pod) {
        Map<String, String> labels = pod.getMetadata().getLabels();

        String runId = labels.get("runId");
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            log.error("Pod Watcher: run {} is empty", runId);
            return null;
        }

        return runOptional.get();
    }

    @Override
    public void onAdd(Pod pod) {
        Run run = getRun(pod);
        if (FINAL_STATES.contains(run.getState())) {
            return;
        }

        run.setState(TaskRunState.PENDING);
        run = runRepository.saveAndFlush(run);
        log.info("Pod Informer: set Run {} to {}", run.getId(), run.getState());
    }

    @Override
    public void onUpdate(Pod oldPod, Pod newPod) {
        Run run = getRun(newPod);
        if (FINAL_STATES.contains(run.getState()) || newPod.getStatus().getPhase().equals("Pending")) {
            return;
        }

        run.setState(STATUS.getOrDefault(newPod.getStatus().getPhase(), TaskRunState.FAILED));
        run = runRepository.saveAndFlush(run);
        log.info("Pod Informer: update Run {} to {}", run.getId(), run.getState());
    }

    @Override
    public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
        log.info("Pod Informer: Pod deleted");
    }
}