package be.cytomine.service.utils;

import java.util.List;

import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.stereotype.Service;

import be.cytomine.exceptions.WrongArgumentException;

@Service
@AllArgsConstructor
public class GeometryService {

    public static final List<String> SUPPORTED_TYPES = List.of(
        "Point",
        "MultiPoint",
        "LineString",
        "MultiLineString",
        "Polygon",
        "MultiPolygon"
    );

    private static Geometry parseWKT(String wkt) {
        try {
            return new WKTReader().read(wkt);
        } catch (ParseException ignored) {
            return null;
        }
    }

    private static Geometry parseGeoJSON(String geojson) {
        try {
            return new GeoJsonReader().read(geojson);
        } catch (ParseException ignored) {
            return null;
        }
    }

    public Boolean isGeometry(String input) {
        Geometry geometry = parseWKT(input);
        if (geometry == null) {
            geometry = parseGeoJSON(input);
        }

        return geometry != null && SUPPORTED_TYPES.contains(geometry.getGeometryType());
    }

    public String WKTToGeoJSON(String wkt) {
        try {
            WKTReader reader = new WKTReader();
            Geometry geometry = reader.read(wkt);

            return new GeoJsonWriter().write(geometry);
        } catch (ParseException e) {
            throw new WrongArgumentException("WKT cannot be convert to GeoJSON: " + wkt);
        }
    }

    public String GeoJSONToWKT(String geoJSON) {
        try {
            GeoJsonReader reader = new GeoJsonReader();
            Geometry geometry = reader.read(geoJSON);

            return new WKTWriter().write(geometry);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid GeoJSON string: " + geoJSON);
        }
    }
}
