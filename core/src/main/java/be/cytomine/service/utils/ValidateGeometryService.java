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

import be.cytomine.exceptions.WrongArgumentException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class ValidateGeometryService {

    EntityManager entityManager;

    // TODO: move this annotation service
    public String tryToMakeItValidIfNotValid(String location) {
        Geometry geometry = null;
        try {
            geometry = tryToMakeItValidIfNotValid(new WKTReader().read(location));
        } catch (Exception e) {
            return location;
        }
        return geometry.toText();
    }


    public Geometry tryToMakeItValidIfNotValid(Geometry location) {
        Geometry result = location;
        try {
            String backupLocation = location.toText();
            Geometry geom = new WKTReader().read(location.toText());
            Geometry validGeom;
            String type = geom.getGeometryType().toUpperCase();

            if (!geom.isValid()) {
                log.info("Geometry is not valid");
                //selfintersect,...
                validGeom = geom.buffer(0);
                result = validGeom;
                geom = new WKTReader().read(result.toText());
                type = geom.getGeometryType().toUpperCase();

                if (!geom.isValid() || geom.isEmpty()) {
                    //if not valid after buffer(0) or empty after buffer 0
                    //user_image already filter nested image

                    log.info("Geometry is not valid, even after a buffer(0)!");
                    String request = "SELECT ST_AsText(ST_MakeValid(ST_AsText('" + backupLocation + "')))";
                    log.info(request);
                    Query nativeQuery = entityManager.createNativeQuery("SELECT ST_AsText(ST_MakeValid(ST_AsText('" + backupLocation + "')))", Tuple.class);
                    List<Tuple> resultList = nativeQuery.getResultList();
                    for (Tuple tuple : resultList) {
                        String text = (String)tuple.get(0);
                        geom = new WKTReader().read(text);
                        type = geom.getGeometryType().toUpperCase();
                        if (type.equals("GEOMETRYCOLLECTION")) {
                            geom = geom.getGeometryN(0);
                            type = geom.getGeometryType().toUpperCase();
                        }
                        result = geom;
                    }

                }
            }

            if (geom.isEmpty()) {
                log.info("Geometry is empty");
                //empty polygon,...
                throw new WrongArgumentException(geom.toText() + " is an empty geometry!");
            }

            //for geometrycollection, we may take first collection element
            if (type.equals("MULTILINESTRING") || type.equals("GEOMETRYCOLLECTION")) {
                //geometry collection, take first elem
                throw new WrongArgumentException(geom.getGeometryType() + " is not a valid geometry type!");
            }
        } catch (ParseException exception) {

        }
        return result;
    }

}
