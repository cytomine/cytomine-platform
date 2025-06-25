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
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

import static be.cytomine.domain.social.PersistentUserPosition.getJtsPolygon;

@Getter
@Setter
@Document
public class LastUserPosition extends CytomineSocialDomain  {

    protected Long id;

    @CreatedDate
    protected Date created;

    @LastModifiedDate
    protected Date updated;

    protected Long user;

    private Long image;

    private Long slice;

    private Long project;

    private String imageName;

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


    public Long computeDateInMillis() {
        return created != null ? created.getTime() - new Date(0).getTime() : null;
    }

    public static JsonObject getDataFromDomain(CytomineSocialDomain domain) {
        JsonObject returnArray = new JsonObject();
        LastUserPosition position = (LastUserPosition)domain;
        returnArray.put("class", position.getClass());
        returnArray.put("id", position.getId());
        returnArray.put("created", DateUtils.getTimeToString(position.created));
        returnArray.put("updated", DateUtils.getTimeToString(position.updated));
        returnArray.put("user", position.getUser());
        returnArray.put("image", position.getImage());
        returnArray.put("slice", position.getSlice());
        returnArray.put("project", position.getProject());
        returnArray.put("zoom", position.getZoom());
        returnArray.put("rotation", position.getRotation());
        returnArray.put("broadcast", position.isBroadcast());
        Polygon polygon = getJtsPolygon(position.location);
        returnArray.put("location", polygon.toString());
        returnArray.put("x", polygon.getCentroid().getX());
        returnArray.put("y", polygon.getCentroid().getY());
        return returnArray;
    }

    public static boolean isSameLocation(List<List<Double>> location1, List<List<Double>> location2){
        return (getX(location1) == getX(location2) && getY(location1) == getY(location2));
    }

    public static double getX(List<List<Double>> location){
        return getJtsPolygon(location).getCentroid().getX();
    }

    public static double getY(List<List<Double>> location){
        return getJtsPolygon(location).getCentroid().getY();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
