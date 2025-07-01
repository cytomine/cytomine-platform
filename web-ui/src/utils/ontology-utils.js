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

// return all terms from an ontology object
export function getAllTerms(ontology) {
  return processNodes(ontology.children.array);
}

function processNodes(nodes) {
  let result = [];
  nodes.forEach(node => {
    result.push(node);
    if (node.children) {
      result.push(...processNodes(node.children));
    }
  });
  return result;
}
