package be.cytomine.appengine.services;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.states.TaskRunState;

@Slf4j
@RequiredArgsConstructor
@Service
public class RunService {

    private final RunRepository runRepository;

    public Run findRun(String runid) {
        return runRepository.findById(UUID.fromString(runid)).orElse(null);
    }

    public Run update(Run run) {
        run.setUpdatedAt(LocalDateTime.now());
        run.setLastStateTransitionAt(LocalDateTime.now());
        return runRepository.saveAndFlush(run);
    }

    public boolean updateRunState(TaskRunState state) {
        log.info("Updating Run State: update to {}", state);
        return true;
    }
}
