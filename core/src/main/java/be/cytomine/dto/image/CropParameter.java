package be.cytomine.dto.image;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CropParameter {

    private String format;

    private String geometry;

    private String location;

    private Boolean complete;

    private Integer maxSize;

    private Integer zoom;

    private Double increaseArea;

    private Boolean safe;

    private Boolean square;

    private String type;

    private Boolean draw;

    private Boolean mask;

    private Boolean alphaMask;

    private Boolean drawScaleBar;

    private Double resolution;

    private Double magnification;

    private String colormap;

    private Boolean inverse;

    private Double contrast;

    private Double gamma;

    private Integer bits;

    private Boolean maxBits;

    private Integer alpha;

    private Integer thickness;

    private String color;

    private Integer jpegQuality;

    private BoundariesCropParameter boundaries;

}
