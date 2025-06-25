package be.cytomine.utils;

import be.cytomine.dto.image.BoundariesCropParameter;

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

import be.cytomine.exceptions.WrongArgumentException;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GeometryUtils {

    public static Geometry createBoundingBox(String bbox) throws ParseException {
        if(bbox.startsWith("POLYGON")) {
            return new WKTReader().read(bbox);
        }
        String[] coordinates = bbox.split(",");
        double minX = Double.parseDouble(coordinates[0]);
        double minY = Double.parseDouble(coordinates[1]);
        double maxX = Double.parseDouble(coordinates[2]);
        double maxY = Double.parseDouble(coordinates[3]);
        return GeometryUtils.createBoundingBox(minX, maxX, minY , maxY);
    }

    public static Geometry createBoundingBox(double minX, double maxX, double minY, double maxY) {
        Coordinate[] roiPoints = new Coordinate[5];
        roiPoints[0] = new Coordinate(minX, minY);
        roiPoints[1] = new Coordinate(minX, maxY);
        roiPoints[2] = new Coordinate(maxX, maxY);
        roiPoints[3] = new Coordinate(maxX, minY);
        roiPoints[4] = roiPoints[0];
        //Build geometry
        LinearRing linearRing = new GeometryFactory().createLinearRing(roiPoints);
        return new GeometryFactory().createPolygon(linearRing);
    }

    public static BoundariesCropParameter getGeometryBoundaries(Geometry geometry) {
        if (geometry.getNumPoints() > 1) {
            Envelope env = geometry.getEnvelopeInternal();
            BoundariesCropParameter cropParameter = new BoundariesCropParameter();
            cropParameter.setTopLeftX((int)Math.round(env.getMinX()));
            cropParameter.setTopLeftY((int)Math.round(env.getMaxY()));
            cropParameter.setWidth((int)env.getWidth());
            cropParameter.setHeight((int)env.getHeight());
            return cropParameter;
        } else if (geometry.getNumPoints() == 1) {
            Envelope env = geometry.getEnvelopeInternal();
            BoundariesCropParameter cropParameter = new BoundariesCropParameter();
            cropParameter.setTopLeftX((int)Math.round(env.getMinX() - 50));
            cropParameter.setTopLeftY((int)Math.round(env.getMaxY() + 50));
            cropParameter.setWidth(100);
            cropParameter.setHeight(100);
            return cropParameter;
        }
        throw new WrongArgumentException("Cannot extract boundaries for " + geometry);
    }


    /**
     * Fill polygon to complete empty space inside polygon/mulypolygon
     * @param polygon A polygon or multipolygon wkt polygon
     * @return A polygon or multipolygon filled points
     */
    public static String fillPolygon(String polygon) {
        if (polygon.startsWith("POLYGON")) return "POLYGON(" + getFirstPolygonLocation(polygon) + ")";
        else if (polygon.startsWith("MULTIPOLYGON")) return "MULTIPOLYGON(" + getFirstPolygonLocationForEachItem(polygon) + ")";
        else throw new WrongArgumentException("Form cannot be filled:" + polygon);
    }

    /**
     * Fill all polygon inside a Multipolygon WKT polygon
     * @param form Multipolygon WKT polygon
     * @return Multipolygon with all its polygon filled
     */
    private static String getFirstPolygonLocationForEachItem(String form) {
        //e.g: "MULTIPOLYGON (((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)) , ((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)) , ((6 3,9 2,9 4,6 3)))";
        String workingForm = form.replaceAll("\\) ", ")");
        //"MULTIPOLYGON(((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)),((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)),((6 3,9 2,9 4,6 3)))";
        workingForm = workingForm.replaceAll(" \\(", "(");
        workingForm = workingForm.replace("MULTIPOLYGON(", "");
        //"((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)),((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)),((6 3,9 2,9 4,6 3)))";
        workingForm = workingForm.substring(0, workingForm.length() - 1);
        //"((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)),((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)),((6 3,9 2,9 4,6 3))";
        String[] polygons = workingForm.split("\\)\\)\\,\\(\\(");
        //"[ ((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2] [1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2] [6 3,9 2,9 4,6 3)) ]";
        List<String> fixedPolygon = new ArrayList<String>();
        for (int i = 0; i < polygons.length; i++) {
            if (i == 0) {
                fixedPolygon.add(polygons[i] + "))");
            } else if (i == polygons.length - 1) {
                fixedPolygon.add("((" + polygons[i] + "");
            } else {
                fixedPolygon.add("((" + polygons[i] + "))");
            }
            //"[ ((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2))] [((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2))] [((6 3,9 2,9 4,6 3)) ]";
        }

        List<String> filledPolygon = new ArrayList<String>();
        for (int i = 0; i < fixedPolygon.size(); i++) {
            filledPolygon.add("(" + getFirstPolygonLocation(fixedPolygon.get(i)) + ")");
            //"[ ((1 1,5 1,5 5,1 5,1 1))] [((1 1,5 1,5 5,1 5,1 1))] [((6 3,9 2,9 4,6 3)) ]";
        }

        String multiPolygon = String.join(",", filledPolygon);
        //"((1 1,5 1,5 5,1 5,1 1)),((1 1,5 1,5 5,1 5,1 1)),((6 3,9 2,9 4,6 3))";
        return multiPolygon;
    }

    /**
     * Fill a polygon
     * @param polygon Polygon as wkt
     * @return Polygon filled points
     */
    private static String getFirstPolygonLocation(String polygon) {
        int i = 0;
        int start, stop;
        while (polygon.charAt(i) != '(') i++;
        while (polygon.charAt(i + 1) == '(') i++;
        start = i;
        while (polygon.charAt(i) != ')') i++;
        stop = i;
        return polygon.substring(start, stop + 1);
    }
}
