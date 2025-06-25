package be.cytomine.domain.social;

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

import be.cytomine.domain.CytomineSocialDomain;
import be.cytomine.utils.DateUtils;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Document
public class PersistentUserPosition extends CytomineSocialDomain {

   // TODO:
   // stateless true //don't store data in memory after read&co. These data don't need to be update.

    //TODO:
    // indexAttributes:[min:Integer.MIN_VALUE, max:Integer.MAX_VALUE],

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @CreatedDate
    protected Date created;

    @LastModifiedDate
    protected Date updated;

    protected Long user;

    private Long image;

    private Long slice;

    private Long project;

    private String session;

    /**
     * User screen area
     */
    @ElementCollection
    List<List<Double>> location;

    /**
     * User zoom on image
     */
    int zoom;

    Double  rotation;

    /**
     * Whether or not the user has decided to broadcast its position
     */
    boolean broadcast;


    private String imageName;

    public Long computeDateInMillis() {
        return created != null ? created.getTime() - new Date(0).getTime() : null;
    }

    public static JsonObject getDataFromDomain(PersistentUserPosition domain) {
        JsonObject returnArray = new JsonObject();
        PersistentUserPosition position = (PersistentUserPosition)domain;
        returnArray.put("class", domain.getClass());
        returnArray.put("id", domain.getId());
        returnArray.put("created", DateUtils.getTimeToString(domain.created));
        returnArray.put("updated", DateUtils.getTimeToString(domain.updated));
        returnArray.put("user", position.getUser()!=null? position.getUser() : null);
        returnArray.put("image", position.getImage()!=null? position.getImage() : null);
        returnArray.put("slice", position.getSlice()!=null? position.getSlice() : null);
        returnArray.put("project", position.getProject()!=null? position.getProject() : null);
        returnArray.put("zoom", position.getZoom());
        returnArray.put("rotation", position.getRotation());
        returnArray.put("broadcast", position.isBroadcast());
        Polygon polygon = getJtsPolygon(domain.location);
        returnArray.put("location", polygon.toString());
        returnArray.put("x", polygon.getCentroid().getX());
        returnArray.put("y", polygon.getCentroid().getY());
        return returnArray;
    }

    public static Polygon getJtsPolygon(GeoJsonPolygon polygon) {
        List<Coordinate> coordinates = polygon.getPoints().stream().map(point -> new Coordinate(point.getX(), point.getY())).collect(Collectors.toList());
        if (coordinates.size() > 1) {
            // finish with the first point
            coordinates.add(coordinates.get(0));
        }
        return new GeometryFactory().createPolygon(coordinates.toArray(Coordinate[]::new));
    }

    public static Polygon getJtsPolygon(List<List<Double>> polygon) {
        List<Coordinate> coordinates = polygon.stream().map(point -> new Coordinate(point.get(0), point.get(1))).collect(Collectors.toList());
        if (coordinates.size() > 1) {
            // finish with the first point
            coordinates.add(coordinates.get(0));
        }
        return new GeometryFactory().createPolygon(coordinates.toArray(Coordinate[]::new));
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
