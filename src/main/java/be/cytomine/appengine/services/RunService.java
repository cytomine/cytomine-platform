package be.cytomine.appengine.services;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.handlers.registry.DockerImage;
import be.cytomine.appengine.dto.inputs.task.*;
import be.cytomine.appengine.dto.misc.TaskIdentifiers;
import be.cytomine.appengine.exceptions.*;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.handlers.FileStorageHandler;
import be.cytomine.appengine.handlers.RegistryHandler;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.repositories.TaskRepository;
import be.cytomine.appengine.states.TaskRunState;
import be.cytomine.appengine.utils.ArchiveUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
public class RunService {

    Logger logger = LoggerFactory.getLogger(RunService.class);

    private final RunRepository runRepository;

    public RunService(RunRepository runRepository) {
        this.runRepository = runRepository;

    }

    public Run findRun(String runid) {
        return runRepository.findById(UUID.fromString(runid)).orElse(null);
    }

    public Run update(Run run) {
        run.setUpdated_at(LocalDateTime.now());
        run.setLast_state_transition_at(LocalDateTime.now());
        return runRepository.saveAndFlush(run);
    }



    public boolean updateRunState(TaskRunState state)
    {
        logger.info("Updating Run State : updating...");
        logger.info("Updating Run State : updated to " + state);
        return true;
    }
}
