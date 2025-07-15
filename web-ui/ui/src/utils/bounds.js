export function isBetweenBounds(val, bounds) {
  return val >= bounds[0] && val <= bounds[1];
}

export function isBoundsFilterActive(bounds, max) {
  return bounds[0] !== 0 || bounds[1] !== max;
}
