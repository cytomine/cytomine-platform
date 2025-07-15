import ModifyInteractionBase from 'ol/interaction/Modify';
import GeometryType from 'ol/geom/GeometryType';
import {equals as coordinatesEqual, distance as coordinateDistance, closestOnSegment} from 'ol/coordinate';
import {getUid} from 'ol/util';
import {boundingExtent} from 'ol/extent';
import {isRectangle} from '@/utils/geometry-utils';

function getSegmentDataUid(segmentData) {
  let uid = getUid(segmentData.feature);
  const depth = segmentData.depth;
  if (depth) {
    uid += '-' + depth.join('-'); // separate feature components
  }

  return uid;
}

/**
 * @param {SegmentData} a The first segment data.
 * @param {SegmentData} b The second segment data.
 * @return {number} The difference in indexes.
 */
function compareIndexes(a, b) {
  return a.index - b.index;
}

/**
 * Returns the point closest to a given line segment.
 *
 * @param {import("ol/coordinate").Coordinate} pointCoordinates The point to which a closest point
 *        should be found.
 * @param {SegmentData} segmentData The object describing the line
 *        segment which should contain the closest point.
 * @return {import("ol/coordinate").Coordinate} The point closest to the specified line segment.
 */
function closestOnSegmentData(pointCoordinates, segmentData) {
  const geometry = segmentData.geometry;

  if (geometry.getType() === GeometryType.CIRCLE &&
    segmentData.index === 1) {
    return geometry.getClosestPoint(pointCoordinates);
  }
  return closestOnSegment(pointCoordinates, segmentData.segment);
}


export default class ModifyInteraction extends ModifyInteractionBase {
  handleDragEvent(evt) {
    this.ignoreNextSingleClick_ = false;
    this.willModifyFeatures_(evt);

    const vertex = evt.coordinate;
    for (let i = 0, ii = this.dragSegments_.length; i < ii; ++i) {
      const dragSegment = this.dragSegments_[i];
      const segmentData = dragSegment[0];
      const depth = segmentData.depth;
      const geometry = segmentData.geometry;
      let coordinates, newVertex;
      const segment = segmentData.segment;
      const index = dragSegment[1];

      while (vertex.length < geometry.getStride()) {
        vertex.push(segment[index][vertex.length]);
      }

      switch (geometry.getType()) {
        case GeometryType.POINT:
          coordinates = vertex;
          segment[0] = segment[1] = vertex;
          break;
        case GeometryType.MULTI_POINT:
          coordinates = geometry.getCoordinates();
          coordinates[segmentData.index] = vertex;
          segment[0] = segment[1] = vertex;
          break;
        case GeometryType.LINE_STRING:
          coordinates = geometry.getCoordinates();
          coordinates[segmentData.index + index] = vertex;
          segment[index] = vertex;
          break;
        case GeometryType.MULTI_LINE_STRING:
          coordinates = geometry.getCoordinates();
          coordinates[depth[0]][segmentData.index + index] = vertex;
          segment[index] = vertex;
          break;
        case GeometryType.POLYGON:
          coordinates = geometry.getCoordinates();
          newVertex = vertex;
          if (i === 2 || i === 3) {
            newVertex = coordinates[depth[0]][segmentData.index + index];
            newVertex[0] = vertex[0];
          } else if (i === 4 || i === 5) {
            newVertex = coordinates[depth[0]][segmentData.index + index];
            newVertex[1] = vertex[1];
          }
          coordinates[depth[0]][segmentData.index + index] = newVertex;
          segment[index] = newVertex;
          break;
        case GeometryType.MULTI_POLYGON:
          coordinates = geometry.getCoordinates();
          coordinates[depth[1]][depth[0]][segmentData.index + index] = vertex;
          segment[index] = vertex;
          break;
        case GeometryType.CIRCLE:
          segment[0] = segment[1] = vertex;
          if (segmentData.index === 1) {
            this.changingFeature_ = true;
            geometry.setCenter(vertex);
            this.changingFeature_ = false;
          } else { // We're dragging the circle's circumference:
            this.changingFeature_ = true;
            geometry.setRadius(coordinateDistance(geometry.getCenter(), vertex));
            this.changingFeature_ = false;
          }
          break;
        default:
        // pass
      }

      if (coordinates) {
        this.setGeometryCoordinates_(geometry, coordinates);
      }
    }
    this.createOrUpdateVertexFeature_(vertex);
  }

  /**
   * @inheritDoc
   */
  handleDownEvent(evt) {
    if (!this.condition_(evt)) {
      return false;
    }
    this.handlePointerAtPixel_(evt.pixel, evt.map);
    const pixelCoordinate = evt.map.getCoordinateFromPixel(evt.pixel);
    this.dragSegments_.length = 0;
    this.modified_ = false;
    const vertexFeature = this.vertexFeature_;
    if (vertexFeature) {
      const insertVertices = [];
      const geometry = /** @type {Point} */ (vertexFeature.getGeometry());
      const vertex = geometry.getCoordinates();
      const vertexExtent = boundingExtent([vertex]);
      const segmentDataMatches = this.rBush_.getInExtent(vertexExtent);
      const componentSegments = {};
      segmentDataMatches.sort(compareIndexes);
      for (let i = 0, ii = segmentDataMatches.length; i < ii; ++i) {
        const segmentDataMatch = segmentDataMatches[i];
        const segment = segmentDataMatch.segment;
        let uid = getSegmentDataUid(segmentDataMatch);
        if (!componentSegments[uid]) {
          componentSegments[uid] = new Array(2);
        }
        if (segmentDataMatch.geometry.getType() === GeometryType.CIRCLE &&
          segmentDataMatch.index === 1) {

          const closestVertex = closestOnSegmentData(pixelCoordinate, segmentDataMatch);
          if (coordinatesEqual(closestVertex, vertex) && !componentSegments[uid][0]) {
            this.dragSegments_.push([segmentDataMatch, 0]);
            componentSegments[uid][0] = segmentDataMatch;
          }
        } else if (segmentDataMatch.geometry.getType() === GeometryType.POLYGON
          && isRectangle(segmentDataMatch.geometry)) {
          let cid = segmentDataMatch.feature.get('annot').id;
          const segmentDataBbox = this.rBush_.getInExtent(segmentDataMatch.geometry.getExtent()).filter(sd => {
            return sd.feature.get('annot').id === cid;
          });

          const otherVertices = segmentDataMatch.geometry.getCoordinates()[0]
            .filter(coord => !coordinatesEqual(coord, vertex));

          const predicates = [
            (sd) => coordinatesEqual(sd.segment[0], vertex)
              && !componentSegments[getSegmentDataUid(sd)][0],
            (sd) => coordinatesEqual(sd.segment[1], vertex)
              && !componentSegments[getSegmentDataUid(sd)][1],
            (sd) => sd.segment[0][0] === vertex[0] && sd.segment[0][1] !== vertex[1]
              && otherVertices.find(c => coordinatesEqual(c, sd.segment[0])),
            (sd) => sd.segment[1][0] === vertex[0] && sd.segment[1][1] !== vertex[1]
              && otherVertices.find(c => coordinatesEqual(c, sd.segment[1])),
            (sd) => sd.segment[0][1] === vertex[1] && sd.segment[0][0] !== vertex[0]
              && otherVertices.find(c => coordinatesEqual(c, sd.segment[0])),
            (sd) => sd.segment[1][1] === vertex[1] && sd.segment[1][0] !== vertex[0]
              && otherVertices.find(c => coordinatesEqual(c, sd.segment[1]))
          ];

          for (let j = 0, jj = predicates.length; j < jj; ++j) {
            const sd = segmentDataBbox.find(predicates[j]);
            if (!sd) {
              break;
            }
            this.dragSegments_.push([sd, j % 2]);
            let uid = getSegmentDataUid(sd);
            if (!componentSegments[uid]) {
              componentSegments[uid] = new Array(2);
            }
            componentSegments[uid][j % 2] = sd;
          }
          break;
        } else if (coordinatesEqual(segment[0], vertex) &&
          !componentSegments[uid][0]) {
          this.dragSegments_.push([segmentDataMatch, 0]);
          componentSegments[uid][0] = segmentDataMatch;
        } else if (coordinatesEqual(segment[1], vertex) &&
          !componentSegments[uid][1]) {

          // prevent dragging closed linestrings by the connecting node
          if ((segmentDataMatch.geometry.getType() ===
            GeometryType.LINE_STRING ||
            segmentDataMatch.geometry.getType() ===
            GeometryType.MULTI_LINE_STRING) &&
            componentSegments[uid][0] &&
            componentSegments[uid][0].index === 0) {
            continue;
          }

          this.dragSegments_.push([segmentDataMatch, 1]);
          componentSegments[uid][1] = segmentDataMatch;
        } else if (this.insertVertexCondition_(evt) && getUid(segment) in this.vertexSegments_ &&
          (!componentSegments[uid][0] && !componentSegments[uid][1])) {
          insertVertices.push([segmentDataMatch, vertex]);
        }
      }
      if (insertVertices.length) {
        this.willModifyFeatures_(evt);
      }
      for (let j = insertVertices.length - 1; j >= 0; --j) {
        this.insertVertex_.apply(this, insertVertices[j]);
      }
    }
    return !!this.vertexFeature_;
  }
}

