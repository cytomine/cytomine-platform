package be.cytomine.appengine.models.task;

import be.cytomine.appengine.models.BaseEntity;
import be.cytomine.appengine.states.TaskRunState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private LocalDateTime last_state_transition_at;
    @ManyToOne(cascade = CascadeType.ALL)
    private Task task;

    private UUID secret;

    @OneToMany(cascade = CascadeType.ALL , fetch = FetchType.EAGER)
    private Set<TypePersistence> provisions;

    public Run(UUID taskRunID, TaskRunState taskRunState, Task task) {
        super();
        this.id = taskRunID;
        this.state = taskRunState;
        this.task = task;
        this.provisions = new HashSet<>();
    }

    public Run(UUID taskRunID, TaskRunState taskRunState, Task task , LocalDateTime created_at , LocalDateTime updated_at , LocalDateTime last_state_transition_at) {
        super();
        this.id = taskRunID;
        this.state = taskRunState;
        this.task = task;
        this.provisions = new HashSet<>();
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.last_state_transition_at = last_state_transition_at;
    }

    public Run(UUID taskRunID, TaskRunState taskRunState, Task task, LocalDateTime created_at) {
        super();
        this.id = taskRunID;
        this.state = taskRunState;
        this.task = task;
        this.provisions = new HashSet<>();
        this.created_at = created_at;
    }
}
