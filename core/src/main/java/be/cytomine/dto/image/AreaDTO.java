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

import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;

import be.cytomine.domain.social.MongodbLocation;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AreaDTO {

    Point topLeft;
    Point topRight;
    Point bottomRight;
    Point bottomLeftX;

    public List<List<Double>> toList() {
        return List.of(topLeft.toList(), topRight.toList(), bottomRight.toList(), bottomLeftX.toList());
    }

    public List<Point> toPointList() {
        return List.of(topLeft, topRight, bottomRight, bottomLeftX);
    }

    public GeoJsonPolygon toGeoJsonPolygon() {
        return new GeoJsonPolygon(toPointList().stream()
                .map(point ->
                        new org.springframework.data.geo.Point(point.getX(), point.getY()))
                .collect(Collectors.toList()));
    }

    public MongodbLocation toMongodbLocation() {
        MongodbLocation location = new MongodbLocation();
        location.setType("polygon");
        location.setCoordinates(toPointList().stream().map(point -> List.of(point.getX(), point.getY())).collect(Collectors.toList()));
        return location;
    }
}
