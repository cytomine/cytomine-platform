package be.cytomine.service.appengine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import be.cytomine.domain.appengine.TaskRunLayer;
import be.cytomine.dto.appengine.task.TaskRunLayerValue;

@RequiredArgsConstructor
@Service
public class TaskRunLayerService {
    public TaskRunLayerValue convertToDTO(TaskRunLayer taskRunLayer) {
        return new TaskRunLayerValue(
            taskRunLayer.getAnnotationLayer().getId(),
            taskRunLayer.getTaskRun().getId(),
            taskRunLayer.getImage().getId(),
            taskRunLayer.getXOffset(),
            taskRunLayer.getYOffset()
        );
    }
}
