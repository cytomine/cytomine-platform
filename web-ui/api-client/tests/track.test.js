import * as utils from './utils.js';
import {Track, TrackCollection} from '@';

describe('Track', function() {

  let project;
  let imageInstance;
  let name = utils.randomString();
  let color = '#ffffff';

  let track = null;
  let id = 0;

  before(async function() {
    await utils.connect(true);
    ({id: imageInstance, project: project} = await utils.getImageInstance());
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      track = new Track({name, image: imageInstance, color});
      track = await track.save();
      id = track.id;
      expect(track).to.be.an.instanceof(Track);
      expect(track.name).to.equal(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedTrack = await Track.fetch(id);
      expect(fetchedTrack).to.be.an.instanceof(Track);
      expect(fetchedTrack.name).to.equal(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedTrack = await new Track({id}).fetch();
      expect(fetchedTrack).to.be.an.instanceof(Track);
      expect(fetchedTrack.name).to.equal(name);
    });

    it('Fetch with wrong ID', function() {
      expect(Track.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newName = utils.randomString();
      track.name = newName;
      track = await track.update();
      expect(track).to.be.an.instanceof(Track);
      expect(track.name).to.equal(newName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Track.delete(id);
    });

    it('Fetch deleted', function() {
      expect(Track.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('TrackCollection', function() {

    let nbTracks = 3;
    let tracks;
    let totalNb = 0;

    before(async function() {
      let trackPromises = [];
      for(let i = 0; i < nbTracks; i++) {
        trackPromises.push(new Track({name: utils.randomString(), image: imageInstance, color}).save());
      }
      tracks = await Promise.all(trackPromises);
    });

    after(async function() {
      let deletionPromises = tracks.map(track => Track.delete(track.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new TrackCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).to.be.an.instanceof(TrackCollection);
        expect(collection).to.have.lengthOf.at.least(nbTracks);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await TrackCollection.fetchAll({filterKey: 'project', filterValue: project});
        expect(collection).to.be.an.instanceof(TrackCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await TrackCollection.fetchAll({filterKey: 'project', filterValue: project, nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(TrackCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await TrackCollection.fetchAll({filterKey: 'project', filterValue: project});
        for(let track of collection) {
          expect(track).to.be.an.instanceof(Track);
        }
      });

      it('Add item to the collection', function() {
        let collection = new TrackCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new Track());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new TrackCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Filtering', function() {
      it('No filter', async function() {
        let collection = new TrackCollection();
        expect(collection.fetchAll()).to.be.rejected;
      });

      it('Filter on imageInstance', async function() {
        let collection = new TrackCollection({filterKey: 'imageinstance', filterValue: imageInstance});
        await collection.fetchAll();
        expect(collection).to.have.lengthOf.at.least(nbTracks);
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new TrackCollection({filterKey: 'project', filterValue: project, nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new TrackCollection({filterKey: 'project', filterValue: project, nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new TrackCollection({filterKey: 'project', filterValue: project, nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});
