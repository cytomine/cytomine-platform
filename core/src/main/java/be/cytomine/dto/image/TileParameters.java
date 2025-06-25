package be.cytomine.dto.image;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TileParameters {

    private String format;

    private Long zoom;

    private Long level;

    private Long tileIndex;

    private Long tx;

    private Long ty;

    private String channels;

    private String zSlices;

    private String timepoints;

    private String filters;

    private String minIntensities;

    private String maxIntensities;

    private String gammas;

    private String colormaps;

}
