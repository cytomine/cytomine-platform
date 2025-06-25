package be.cytomine.domain.appengine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.annotation.AnnotationLayer;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.utils.JsonObject;

@Setter
@Getter
@Entity
public class TaskRunLayer extends CytomineDomain {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "layer_id")
    private AnnotationLayer annotationLayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_run_id")
    private TaskRun taskRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_instance_id")
    private ImageInstance image;

    @Column(name = "x_offset")
    private Integer xOffset;

    @Column(name = "y_offset")
    private Integer yOffset;

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        TaskRunLayer taskRunLayer = (TaskRunLayer) domain;
        JsonObject domainData = CytomineDomain.getDataFromDomain(domain);
        domainData.put("annotationLayer", taskRunLayer.getAnnotationLayer().getId());
        domainData.put("taskRun", taskRunLayer.getTaskRun().getId());
        domainData.put("image", taskRunLayer.getImage().getId());
        domainData.put("xOffset", taskRunLayer.getXOffset());
        domainData.put("yOffset", taskRunLayer.getYOffset());

        return domainData;
    }

    @Override
    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        TaskRunLayer taskRunLayer = this;
        taskRunLayer.id = json.getJSONAttrLong("id", null);
        taskRunLayer.annotationLayer = (AnnotationLayer) json.getJSONAttrDomain(entityManager, "annotationLayer", new AnnotationLayer(), true);
        taskRunLayer.taskRun = (TaskRun) json.getJSONAttrDomain(entityManager, "taskRun", new TaskRun(), true);
        taskRunLayer.image = (ImageInstance) json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);
        taskRunLayer.xOffset = json.getJSONAttrInteger("xOffset");
        taskRunLayer.yOffset = json.getJSONAttrInteger("yOffset");
        taskRunLayer.created = json.getJSONAttrDate("created");
        taskRunLayer.updated = json.getJSONAttrDate("updated");

        return taskRunLayer;
    }

    @Override
    public CytomineDomain container() {
        return this;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
