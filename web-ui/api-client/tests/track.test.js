import * as utils from './utils.js';
import {Track, TrackCollection} from '@/index.js';

describe('Track', () => {

  let project;
  let imageInstance;
  let name = utils.randomString();
  let color = '#ffffff';

  let track = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect(true);
    ({id: imageInstance, project: project} = await utils.getImageInstance());
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      track = new Track({name, image: imageInstance, color});
      track = await track.save();
      id = track.id;
      expect(track).toBeInstanceOf(Track);
      expect(track.name).toEqual(name);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedTrack = await Track.fetch(id);
      expect(fetchedTrack).toBeInstanceOf(Track);
      expect(fetchedTrack.name).toEqual(name);
    });

    it('Fetch with instance method', async () => {
      let fetchedTrack = await new Track({id}).fetch();
      expect(fetchedTrack).toBeInstanceOf(Track);
      expect(fetchedTrack.name).toEqual(name);
    });

    it('Fetch with wrong ID', async () => {
      await expect(Track.fetch(0)).rejects.toThrow();
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newName = utils.randomString();
      track.name = newName;
      track = await track.update();
      expect(track).toBeInstanceOf(Track);
      expect(track.name).toEqual(newName);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await Track.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(Track.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('TrackCollection', () => {

    let nbTracks = 3;
    let tracks;
    let totalNb = 0;

    beforeAll(async () => {
      let trackPromises = [];
      for (let i = 0; i < nbTracks; i++) {
        trackPromises.push(new Track({name: utils.randomString(), image: imageInstance, color}).save());
      }
      tracks = await Promise.all(trackPromises);
    });

    afterAll(async () => {
      let deletionPromises = tracks.map(track => Track.delete(track.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new TrackCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).toBeInstanceOf(TrackCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbTracks);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await TrackCollection.fetchAll({filterKey: 'project', filterValue: project});
        expect(collection).toBeInstanceOf(TrackCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await TrackCollection.fetchAll({filterKey: 'project', filterValue: project, nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(TrackCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await TrackCollection.fetchAll({filterKey: 'project', filterValue: project});
        for (let track of collection) {
          expect(track).toBeInstanceOf(Track);
        }
      });

      it('Add item to the collection', () => {
        let collection = new TrackCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Track());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new TrackCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Filtering', () => {
      it('No filter', async () => {
        let collection = new TrackCollection();
        await expect(collection.fetchAll()).rejects.toThrow();
      });

      it('Filter on imageInstance', async () => {
        let collection = new TrackCollection({filterKey: 'imageinstance', filterValue: imageInstance});
        await collection.fetchAll();
        expect(collection.length).toBeGreaterThanOrEqual(nbTracks);
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new TrackCollection({filterKey: 'project', filterValue: project, nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new TrackCollection({filterKey: 'project', filterValue: project, nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new TrackCollection({filterKey: 'project', filterValue: project, nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});
