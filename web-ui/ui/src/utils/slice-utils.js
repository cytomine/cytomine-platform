export function formatMinutesSeconds(time) {
  let minutes = Math.floor(time / 60);
  let remainder = time - minutes * 60;
  let seconds = Math.floor(remainder);
  let milliseconds = Math.floor((remainder - seconds) * 1000);
  return [
    minutes.toString().padStart(2, '0'),
    seconds.toString().padStart(2, '0'),
    milliseconds.toString().padStart(3, '0')
  ].join(':');
}

export function slicePositionToRank({channel, zStack, time}, image) {
  return channel + image.channels * (zStack + image.depth * time);
}
