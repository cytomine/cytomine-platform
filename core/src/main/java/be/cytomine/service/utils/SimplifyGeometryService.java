package be.cytomine.service.utils;

import be.cytomine.dto.annotation.SimplifiedAnnotation;
import be.cytomine.exceptions.WrongArgumentException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SimplifyGeometryService {

    @Value("${application.annotation.maxNumberOfPoint}")
    Double maxNumberOfPoint;

    public SimplifiedAnnotation simplifyPolygon(String form, Long minPoint, Long maxPoint) {
        try {
            return simplifyPolygon(new WKTReader().read(form), minPoint, maxPoint);
        } catch (ParseException e) {
            throw new WrongArgumentException("Annotation cannot be parsed "+ form);
        }
    }

    /**
     * Simplify form (limit point number)
     * Return simplify polygon and the rate used for simplification
     */
    public SimplifiedAnnotation simplifyPolygon(Geometry geometry, Long minPoint, Long maxPoint) {
        // Fast response for simple geometries
        if (geometry.getNumPoints() < 100)
            return new SimplifiedAnnotation(geometry, 0.0d);

        int numOfGeometry = 0;
        if (geometry instanceof MultiPolygon) {
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                Geometry geom = geometry.getGeometryN(i);
                int nbInteriorRing = 1;
                if(geom instanceof Polygon) {
                    nbInteriorRing = ((Polygon)geom).getNumInteriorRing();
                }
                numOfGeometry +=  geom.getNumGeometries() * nbInteriorRing;
            }
        } else {
            int nbInteriorRing = 1;
            if(geometry instanceof Polygon)
                nbInteriorRing = ((Polygon)geometry).getNumInteriorRing();
            numOfGeometry = geometry.getNumGeometries() * nbInteriorRing;
        }
        numOfGeometry = Math.max(1, numOfGeometry);

        if (numOfGeometry > 10) {
            numOfGeometry = numOfGeometry / 2;
        }
        numOfGeometry = Math.min(10, numOfGeometry);

        double ratioMax = 1.3d;
        double ratioMin = 1.7d;
        Double maxNumberOfPoint = this.maxNumberOfPoint;
        Double minNumberOfPoint = 100.0d;

        double numberOfPoint = geometry.getNumPoints();

        /* Maximum number of point that we would have (500/5 (max 150)=max 100 points)*/
        double rateLimitMax;
        if (maxPoint!=null && maxPoint!=0) {
            rateLimitMax = maxPoint * numOfGeometry;
        } else {
            rateLimitMax = Math.max(numberOfPoint / ratioMax, numOfGeometry * maxNumberOfPoint);
        }

        /* Minimum number of point that we would have (500/10 (min 10 max 100)=min 50 points)*/
        double rateLimitMin;
        if (minPoint!=null) {
            rateLimitMin = minPoint * numOfGeometry;
        } else {
            rateLimitMin = Math.min(Math.max(numberOfPoint / ratioMin, 10), numOfGeometry * minNumberOfPoint);
        }

        /* Increase value for the increment (allow to converge faster) */
        double incrThreshold = 0.25d;
        double i = 0;
        double rate = 0;

        /* Max number of loop (prevent infinite loop) */
        int maxLoop = 1000;

        Geometry newGeometry = (Geometry)geometry.clone();
        Boolean isPolygonAndNotValid = (geometry instanceof Polygon && !((Polygon) geometry).isValid());
        Boolean isMultiPolygon = (geometry instanceof MultiPolygon);
        while (numberOfPoint > rateLimitMax && maxLoop > 0) {
            rate = i;
            if (isPolygonAndNotValid || isMultiPolygon) {
                newGeometry = TopologyPreservingSimplifier.simplify(geometry, rate);
            } else {
                newGeometry = DouglasPeuckerSimplifier.simplify(geometry, rate);
            }

            if (newGeometry.getNumPoints() < rateLimitMin)
                break;

            i = i + ((incrThreshold));
            numberOfPoint = newGeometry.getNumPoints();
            maxLoop--;
        }
        return new SimplifiedAnnotation(newGeometry, rate);
    }


    public SimplifiedAnnotation simplifyPolygon(String form, double rate) {
        try {
            return simplifyPolygon(new WKTReader().read(form), rate);
        } catch (ParseException e) {
            throw new WrongArgumentException("Annotation cannot be parsed "+ form);
        }
    }

    public SimplifiedAnnotation simplifyPolygon(Geometry geometry, double rate) {
        Geometry newGeometry;
        Boolean isPolygonAndNotValid = (geometry instanceof Polygon && !((Polygon) geometry).isValid());
        Boolean isMultiPolygon = (geometry instanceof MultiPolygon);
        if (isPolygonAndNotValid || isMultiPolygon) {
            newGeometry = TopologyPreservingSimplifier.simplify(geometry, rate);
        } else {
            newGeometry = DouglasPeuckerSimplifier.simplify(geometry, rate);;
        }
        return new SimplifiedAnnotation(newGeometry, rate);
    }


    public Geometry simplifyPolygonForCrop(Geometry geometry) {
        if (geometry.getNumPoints() < 100) {
            return reduceGeometryPrecision(geometry);
        }

        double index = 2d;
        int max = 1000;
        while (index < max) {
            geometry = TopologyPreservingSimplifier.simplify(geometry, index);
            if (geometry.getNumPoints() < 150) {
                break;
            }
            index = (index + 5) * 1.1;
        }

        return reduceGeometryPrecision(geometry);
    }

    public Geometry reduceGeometryPrecision(Geometry geometry) {
        return reduceGeometryPrecision(geometry, 100);
    }

    public Geometry reduceGeometryPrecision(Geometry geometry, int scale) {
        GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(new PrecisionModel(scale));
        return reducer.reduce(geometry).norm();
    }


}
