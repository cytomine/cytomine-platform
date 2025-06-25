package be.cytomine.service.utils;

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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.TestUtils;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.dto.annotation.SimplifiedAnnotation;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class SimplifyGeometryServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SimplifyGeometryService simplifyGeometryService;

    @Test
    public void simplify_big_annotation() throws ParseException {

        //create annotation
        UserAnnotation annotation = builder.given_a_user_annotation();

        //add very big geometry
        annotation.setLocation(new WKTReader().read(TestUtils.getResourceFileAsString("dataset/big_annotation.txt")));

        assertThat(annotation.getLocation().getNumPoints()).isGreaterThanOrEqualTo(500);

        long maxPoint;
        long minPoint;

        //simplify
        maxPoint = 150;
        minPoint = 100;

        SimplifiedAnnotation result = simplifyGeometryService.simplifyPolygon(annotation.getLocation(), minPoint, maxPoint);

        assertThat(result.getNewAnnotation().getNumPoints()).isLessThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), maxPoint));
        assertThat(result.getNewAnnotation().getNumPoints()).isGreaterThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), minPoint));

        maxPoint = 1000;
        minPoint = 400;

        result = simplifyGeometryService.simplifyPolygon(annotation.getLocation(), minPoint, maxPoint);

        assertThat(result.getNewAnnotation().getNumPoints()).isLessThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), maxPoint));
        assertThat(result.getNewAnnotation().getNumPoints()).isGreaterThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), minPoint));

        maxPoint = 1000;
        minPoint = 400;

        result = simplifyGeometryService.simplifyPolygon(annotation.getLocation(), minPoint, maxPoint);

        assertThat(result.getNewAnnotation().getNumPoints()).isLessThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), maxPoint));
        assertThat(result.getNewAnnotation().getNumPoints()).isGreaterThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), minPoint));
    }

    @Test
    public void simplify_very_big_annotation() throws ParseException {

        //create annotation
        UserAnnotation annotation = builder.given_a_user_annotation();

        //add very big geometry
        annotation.setLocation(new WKTReader().read(TestUtils.getResourceFileAsString("dataset/very_big_annotation.txt")));

        assertThat(annotation.getLocation().getNumPoints()).isGreaterThanOrEqualTo(500);

        long maxPoint;
        long minPoint;

        //simplify
        maxPoint = 50;
        minPoint = 10;

        SimplifiedAnnotation result = simplifyGeometryService.simplifyPolygon(annotation.getLocation(), minPoint, maxPoint);

        assertThat(result.getNewAnnotation().getNumPoints()).isLessThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), maxPoint));
        assertThat(result.getNewAnnotation().getNumPoints()).isGreaterThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), minPoint));
    }

    @Test
    public void simplify_annotation_with_empty_space() throws ParseException {

        //create annotation
        UserAnnotation annotation = builder.given_a_user_annotation();

        //add very big geometry
        annotation.setLocation(new WKTReader().read(TestUtils.getResourceFileAsString("dataset/annotationbig_emptyspace.txt")));

        assertThat(annotation.getLocation().getNumPoints()).isGreaterThanOrEqualTo(500);

        long maxPoint;
        long minPoint;

        //simplify
        maxPoint = 5000*10;
        minPoint = 1000;

        SimplifiedAnnotation result = simplifyGeometryService.simplifyPolygon(annotation.getLocation(), minPoint, maxPoint);

        assertThat(result.getNewAnnotation().getNumPoints()).isLessThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), maxPoint));
        assertThat(result.getNewAnnotation().getNumPoints()).isGreaterThanOrEqualTo((int)getPointMultiplyByGeometriesOrInteriorRings(annotation.getLocation(), minPoint));
    }

    @Test
    public void simplify_annotation_with_rate() throws ParseException {

        Geometry expected = new WKTReader().read("POLYGON ((120 120, 140 199, 160 200, 180 199, 220 120, 120 120))").norm();
        Double geometryCompression = 10.0;

        String location = "POLYGON ((120 120, 121 121, 122 122, 220 120, 180 199, 160 200, 140 199, 120 120))";

        SimplifiedAnnotation simplifiedAnnotation = simplifyGeometryService.simplifyPolygon(location, geometryCompression);

        assertThat(simplifiedAnnotation.getNewAnnotation().norm().toText()).isEqualTo(expected.toText());

    }


    public static int getPointMultiplyByGeometriesOrInteriorRings(Geometry geometry, long numberOfPoints){
        int result = 0;
        if (geometry instanceof MultiPolygon) {
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                Geometry geom = geometry.getGeometryN(i);
                int nbInteriorRing = 1;
                if(geom instanceof Polygon)
                    nbInteriorRing = ((Polygon)geom).getNumInteriorRing();
                result +=  geom.getNumGeometries() * nbInteriorRing;
            }
        } else {
            int nbInteriorRing = 1;
            if(geometry instanceof Polygon)
                nbInteriorRing = ((Polygon)geometry).getNumInteriorRing();
            result = geometry.getNumGeometries() * nbInteriorRing;
        }
        result = Math.max(1, result);

        if (result > 10) result/= 2;
        result = Math.min(10, result);

        result*=numberOfPoints;
        return result;
    }
}
