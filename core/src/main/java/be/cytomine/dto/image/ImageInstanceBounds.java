package be.cytomine.dto.image;

import java.util.Date;

import lombok.Getter;

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.dto.AbstractBounds;
import be.cytomine.utils.MinMax;

@Getter
public class ImageInstanceBounds extends AbstractBounds {

    private MinMax<Date> created = new MinMax<>(); // should be long in final request!!

    private MinMax<Date> updated = new MinMax<>();

    private MinMax<Date> reviewStart = new MinMax<>();

    private MinMax<Date> reviewStop = new MinMax<>();

    private MinMax<Integer> magnification = new MinMax<>(); // + list

    private MinMax<Double> physicalSizeX = new MinMax<>();

    private MinMax<Double> physicalSizeY = new MinMax<>();

    private MinMax<Double> physicalSizeZ = new MinMax<>();

    private MinMax<Double> fps = new MinMax<>();

    private MinMax<String> format = new MinMax<>();

    private MinMax<String> mimeType = new MinMax<>();

    private MinMax<Long> countImageAnnotations = new MinMax<>();

    private MinMax<Long> countImageJobAnnotations = new MinMax<>();

    private MinMax<Long> countImageReviewedAnnotations = new MinMax<>();

    private MinMax<Integer> width = new MinMax<>();

    private MinMax<Integer> height = new MinMax<>();

    private MinMax<String> instanceFilename = new MinMax<>();

    public void submit(ImageInstance imageInstance) {
        AbstractImage abstractImage = imageInstance.getBaseImage();
        updateMinMax(created, imageInstance.getCreated());
        updateMinMax(updated, imageInstance.getUpdated());
        updateMinMax(reviewStart, imageInstance.getReviewStart());
        updateMinMax(reviewStop, imageInstance.getReviewStop());

        updateMinMax(magnification, imageInstance.getMagnification());
        updateChoices(magnification, imageInstance.getMagnification());

        updateMinMax(physicalSizeX, imageInstance.getPhysicalSizeX());
        updateChoices(physicalSizeX, imageInstance.getPhysicalSizeX());

        updateMinMax(physicalSizeY, imageInstance.getPhysicalSizeY());
        updateMinMax(physicalSizeZ, imageInstance.getPhysicalSizeZ());
        updateMinMax(fps, imageInstance.getFps());

        updateChoices(mimeType, abstractImage.getContentType());
        updateChoices(format, abstractImage.getContentType());

        updateMinMax(countImageAnnotations, imageInstance.getCountImageAnnotations());
        updateMinMax(countImageJobAnnotations, imageInstance.getCountImageJobAnnotations());
        updateMinMax(countImageReviewedAnnotations, imageInstance.getCountImageReviewedAnnotations());

        updateMinMax(width, abstractImage.getWidth());
        updateMinMax(height, abstractImage.getHeight());

        updateMinMax(instanceFilename, imageInstance.getInstanceFilename());
    }
}
