import randomstring from 'randomstring';

import Cytomine from '@/cytomine.js';
import config from './config.js';
import * as cytomine from '@/index.js';

let createdModels = [];

export async function wait(ms) {
  return new Promise(r => setTimeout(r, ms));
}

export async function connect(adminSession = false) {
  let cytomineInstance = new Cytomine(config.host, config.basePath);
  await cytomineInstance.login(config.username, config.password);
  if (adminSession) {
    await cytomineInstance.openAdminSession();
  }
}

export function randomString() {
  return `TEST_JS_${randomstring.generate(10)}`;
}

async function getModel(model, collection, forceCreation) {
  if (!forceCreation) {
    await collection.fetchPage();
    if (collection.length > 0) {
      return collection.get(0);
    }
  }
  // if no model was found or if a new model is explicitly required with the forceCreation parameter
  await model.save();
  createdModels.push(model);
  return model;
}

export async function getAbstractImage({filename = randomString(), uploadedFile, forceCreation = true, cascadeForceCreation} = {}) {
  if (!uploadedFile) {
    if (!forceCreation) {
      throw new Error('Cannot retrieve abstract image without uploaded file. Either set forceCreation to true or provide an uploaded file');
    }
    ({id: uploadedFile} = await getUploadedFile({filename, forceCreation: cascadeForceCreation, cascadeForceCreation}));
  }

  let abstractImage = new cytomine.AbstractImage({originalFilename: filename, uploadedFile, width: 1000, height: 1000});
  let abstractImageCollection = new cytomine.AbstractImageCollection({nbPerPage: 1});
  abstractImage = await getModel(abstractImage, abstractImageCollection, forceCreation);

  await new cytomine.AbstractSlice({uploadedFile, image: abstractImage.id, mime: 'image/pyrtiff'}).save();
  return abstractImage;
}

// WARNING: no creation, the instances must exist
export async function getMultipleAbstractImages(nb) {
  let collection = new cytomine.AbstractImageCollection({nbPerPage: nb});
  await collection.fetchPage();
  if (collection.length < nb) {
    throw new Error(`Not able to retrieve ${nb} abstract images. You may need to upload some on test instance.`);
  }
  let ids = [];
  for (let item of collection) {
    ids.push(item.id);
  }
  return ids;
}

export async function getAnnotation({location = 'POINT(5 5)', image, forceCreation = true, cascadeForceCreation} = {}) {
  let annotationCollection = new cytomine.AnnotationCollection({image});
  if (!image) {
    if (!forceCreation) {
      throw new Error('Cannot retrieve annotation without base image. Either set forceCreation to true or provide an image');
    }
    ({id: image} = await getImageInstance({forceCreation: cascadeForceCreation, cascadeForceCreation}));
  }

  let annotation = new cytomine.Annotation({location, image});
  return getModel(annotation, annotationCollection, forceCreation);
}

export async function getTrack({name = randomString(), image, color = '#ffffff', forceCreation = true, cascadeForceCreation} = {}) {
  if (!image) {
    if (!forceCreation) {
      throw new Error('Cannot retrieve track without base image. Either set forceCreation to true or provide an image');
    }
    ({id: image} = await getImageInstance({forceCreation: cascadeForceCreation, cascadeForceCreation}));
  }

  let track = new cytomine.Track({name, image, color});
  let trackCollection = new cytomine.TrackCollection({image});
  return getModel(track, trackCollection, forceCreation);
}

export async function getImageInstance({baseImage, project, forceCreation = true, cascadeForceCreation} = {}) {
  if (!forceCreation && baseImage) {
    throw new Error('Cannot retrieve image instance of a given base image. Either set forceCreation to true or remove baseImage');
  }

  if (!baseImage) {
    ({id: baseImage} = await getAbstractImage({forceCreation: cascadeForceCreation}));
  }

  let imageCollection = new cytomine.ImageInstanceCollection({nbPerPage: 1});
  if (!project) {
    ({id: project} = await getProject({forceCreation: cascadeForceCreation, cascadeForceCreation}));
  } else {
    imageCollection.setFilter('project', project);
  }

  let image = new cytomine.ImageInstance({baseImage, project});
  return getModel(image, imageCollection, forceCreation);
}

// WARNING: if an ontology is created, it may not be possible to delete it afterwards (bug in core preventing deletion
// if ontology used in deleted project) => leave forceCreation to false if possible
export async function getOntology({name = randomString(), forceCreation = false} = {}) {
  let ontology = new cytomine.Ontology({name});
  let ontologyCollection = new cytomine.OntologyCollection({nbPerPage: 1});
  return getModel(ontology, ontologyCollection, forceCreation);
}

export async function getProject({name = randomString(), ontology, forceCreation = true, cascadeForceCreation} = {}) {
  let projectCollection = new cytomine.ProjectCollection({nbPerPage: 1});
  if (!ontology) {
    ({id: ontology} = await getOntology({forceCreation: cascadeForceCreation}));
  } else {
    projectCollection.setFilter('ontology', ontology);
  }
  let project = new cytomine.Project({name, ontology});
  return getModel(project, projectCollection, forceCreation);
}

export async function getRole() {
  let collection = new cytomine.RoleCollection({nbPerPage: 1});
  return getModel(null, collection, false);
}

export async function getMultipleRoles(nb) {
  let collection = new cytomine.RoleCollection({nbPerPage: nb});
  await collection.fetchPage();
  if (collection.length < nb) {
    throw new Error(`Not able to retrieve ${nb} roles.`);
  }
  let ids = [];
  for (let item of collection) {
    ids.push(item.id);
  }
  return ids;
}

// WARNING: bug in core prevents the deletion of storage => it is advised to leave user field to null, so that the
// deletion of the user during clean-up triggers the deletion of the storage
export async function getStorage({user, name = randomString(), forceCreation = true, cascadeForceCreation} = {}) {
  if (!forceCreation && user) {
    throw new Error('Cannot retrieve storage of a given user. Either set forceCreation to true or remove user.');
  }

  let storageCollection = new cytomine.StorageCollection({nbPerPage: 1});
  if (!user) {
    ({id: user} = await getUser({forceCreation: cascadeForceCreation}));
  }

  let storage = new cytomine.Storage({user, name});
  return getModel(storage, storageCollection, forceCreation);
}

export async function getTerm({name = randomString(), ontology, color = '#ffffff', forceCreation = true, cascadeForceCreation} = {}) {
  let termCollection = new cytomine.TermCollection({nbPerPage: 1});
  if (!ontology) {
    ({id: ontology} = await getOntology({forceCreation: cascadeForceCreation}));
  } else {
    termCollection.setFilter('ontology', ontology);
  }
  let term = new cytomine.Term({name, ontology, color});
  return getModel(term, termCollection, forceCreation);
}

export async function getUser({username = randomString(), password, email, firstname, lastname, forceCreation = true} = {}) {
  password = password || username;
  firstname = firstname || username;
  lastname = lastname || username;
  email = email || (username + '@cytomine.coop');

  let userCollection = new cytomine.UserCollection({nbPerPage: 1});
  let user = new cytomine.User({username, password, firstname, lastname, email});
  return getModel(user, userCollection, forceCreation);
}
export async function getTag({name = randomString(), forceCreation = true} = {}) {
  let tagCollection = new cytomine.TagCollection({nbPerPage: 1});
  let tag = new cytomine.Tag({name});
  return getModel(tag, tagCollection, forceCreation);
}

export async function getUploadedFile({storage, filename, originalFilename, ext, contentType, forceCreation = true, cascadeForceCreation} = {}) {
  let user;
  if (!storage) {
    ({id: storage, user: user} = await getStorage(cascadeForceCreation));
  } else {
    ({user: user} = await cytomine.Storage.fetch(storage));
  }

  filename = filename || randomString();
  originalFilename = originalFilename || filename;
  ext = ext || '.ext';
  contentType = contentType || 'contentType';

  let uploadedFileCollection = new cytomine.UploadedFileCollection({nbPerPage: 1});
  let uploadedFile = new cytomine.UploadedFile({storage, user, filename, originalFilename, contentType, ext});
  return getModel(uploadedFile, uploadedFileCollection, forceCreation);
}

export async function cleanData() {
  await Cytomine.instance.openAdminSession();

  // delete models sequentially and in reverse order to ensure there is no foreign key constraint issues
  for (let i = createdModels.length - 1; i >= 0; i--) {
    let model = createdModels[i];
    await model.delete();
  }
  createdModels = [];
}
