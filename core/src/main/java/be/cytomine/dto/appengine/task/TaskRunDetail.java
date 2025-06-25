package be.cytomine.dto.appengine.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class TaskRunDetail {
    private Long project;

    private Long user;

    private Long image;

    private String taskRunId;
}
