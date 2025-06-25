package be.cytomine.dto.appengine.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@RequiredArgsConstructor
public class TaskRunLayerValue {
    private final Long annotationLayer;

    private final Long taskRun;

    private final Long image;

    private final Integer xOffset;

    private final Integer yOffset;
}
