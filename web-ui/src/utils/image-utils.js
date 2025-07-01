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

function isWebPSupported() {
  let elem = document.createElement('canvas');
  if (elem.getContext && elem.getContext('2d')) {
    return elem.toDataURL('image/webp').indexOf('data:image/webp') === 0;
  }

  // very old browser like IE 8, canvas not supported
  return false;
}

export const SUPPORT_WEBP = isWebPSupported();
export const IMAGE_FORMAT = (SUPPORT_WEBP) ? 'webp' : 'jpg';

export function splitImageUrl(rawUrl) {
  let url = new URL(rawUrl);
  let pathname = url.pathname.split('.')[0];
  let params = url.searchParams;

  return {
    host: `${url.protocol}//${url.host}`,
    pathname: pathname,
    params: params
  };
}

export function combineImageUrl({host, pathname, format, params}) {
  if (!(params instanceof URLSearchParams)) {
    params = new URLSearchParams(params);
  }
  let formattedParams = params.toString();
  let sep = (formattedParams.length > 0) ? '?' : '';
  return `${host}${pathname}.${format}${sep}${formattedParams}`;
}


export function changeImageUrlFormat(url, newFormat = IMAGE_FORMAT) {
  return combineImageUrl({format: newFormat, ...splitImageUrl(url)});
}
