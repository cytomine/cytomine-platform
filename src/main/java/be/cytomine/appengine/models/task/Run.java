package be.cytomine.appengine.models.task;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.models.BaseEntity;
import be.cytomine.appengine.states.TaskRunState;

@Entity
@Table(name = "run")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Run extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private TaskRunState state;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastStateTransitionAt;

    @ManyToOne(cascade = CascadeType.ALL)
    private Task task;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<TypePersistence> provisions;

    public Run(UUID taskRunId, TaskRunState taskRunState, Task task) {
        super();
        this.id = taskRunId;
        this.state = taskRunState;
        this.task = task;
        this.provisions = new HashSet<>();
    }

    public Run(
        UUID taskRunId,
        TaskRunState taskRunState,
        Task task,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastStateTransitionAt
    ) {
        super();
        this.id = taskRunId;
        this.state = taskRunState;
        this.task = task;
        this.provisions = new HashSet<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastStateTransitionAt = lastStateTransitionAt;
    }

    public Run(UUID taskRunId, TaskRunState taskRunState, Task task, LocalDateTime createdAt) {
        super();
        this.id = taskRunId;
        this.state = taskRunState;
        this.task = task;
        this.provisions = new HashSet<>();
        this.createdAt = createdAt;
    }
}
