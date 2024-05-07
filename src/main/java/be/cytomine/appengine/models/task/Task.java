package be.cytomine.appengine.models.task;

import be.cytomine.appengine.models.BaseEntity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "task", uniqueConstraints = @UniqueConstraint(columnNames = {"namespace", "version"}))
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Task extends BaseEntity {

    @Id
    @Column(name = "identifier", updatable = false, nullable = false)
    UUID identifier;
    // together they allow to uniquely identify a task across the world
    private String namespace;
    private String version;
    // the task descriptor file object reference (id) in the file storage
    private String descriptorFile;
    // the task bucket reference (id)
    private String storageReference;
    private String description;
    private String name;
    private String nameShort;
    private String inputFolder;
    private String outputFolder;
    private String imageName;

    @OneToMany(cascade = CascadeType.ALL , fetch = FetchType.LAZY)
    private Set<Author> authors;
    @OneToMany(cascade = CascadeType.ALL , fetch = FetchType.EAGER)
    private Set<Input> inputs;
    @OneToMany(cascade = CascadeType.ALL , fetch = FetchType.LAZY)
    private Set<Output> outputs;
    @OneToMany(cascade = CascadeType.ALL , fetch = FetchType.EAGER)
    private List<Run> runs;


    public Task(UUID identifier, String namespace, String version, String descriptorFile, String storageReference) {
        this.identifier = identifier;
        this.namespace = namespace;
        this.version = version;
        this.descriptorFile = descriptorFile;
        this.storageReference = storageReference;
    }


}


