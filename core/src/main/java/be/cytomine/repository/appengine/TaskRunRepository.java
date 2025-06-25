package be.cytomine.repository.appengine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import be.cytomine.domain.appengine.TaskRun;

public interface TaskRunRepository extends JpaRepository<TaskRun, Long>, JpaSpecificationExecutor<TaskRun> {
    Optional<TaskRun> findByProjectIdAndTaskRunId(Long projectId, UUID taskRunId);

    List<TaskRun> findAllByProjectId(Long projectId);
}
