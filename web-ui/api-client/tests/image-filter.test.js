describe('Image filter', function() {
  /*beforeAll(async function() {
    await utils.connect(true);
    processingServer = await utils.getProcessingServer();
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      imageFilter = new ImageFilter({name, processingServer: processingServer.url, baseUrl: 'path/'});
      imageFilter = await imageFilter.save();
      id = imageFilter.id;
      expect(imageFilter).toBeInstanceOf(ImageFilter);
      expect(imageFilter.name).toEqual(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedImageFilter = await ImageFilter.fetch(id);
      expect(fetchedImageFilter).toBeInstanceOf(ImageFilter);
      expect(fetchedImageFilter.name).toEqual(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedImageFilter = await new ImageFilter({id}).fetch();
      expect(fetchedImageFilter).toBeInstanceOf(ImageFilter);
      expect(fetchedImageFilter.name).toEqual(name);
    });

    it('Fetch with wrong ID', function() {
      expect(ImageFilter.fetch(0)).rejects.toThrow();
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      expect(imageFilter.update.bind(imageFilter)).toThrow();
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await ImageFilter.delete(id);
    });

    it('Fetch deleted', function() {
      expect(ImageFilter.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('ImageFilterCollection', function() {

    let nbImageFilters = 3;
    let imageFilters;
    let totalNb = 0;

    beforeAll(async function() {
      let imageFilterPromises = [];
      for(let i = 0; i < nbImageFilters; i++) {
        imageFilterPromises.push(new ImageFilter({
          name: utils.randomString(),
          processingServer: processingServer.url,
          baseUrl: 'path/'
        }).save());
      }
      imageFilters = await Promise.all(imageFilterPromises);
    });

    afterAll(async function() {
      let deletionPromises = imageFilters.map(imageFilter => ImageFilter.delete(imageFilter.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ImageFilterCollection().fetchAll();
        expect(collection).toBeInstanceOf(ImageFilterCollection);
        expect(collection).toBeGreaterThanOrEqual(nbImageFilters);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await ImageFilterCollection.fetchAll();
        expect(collection).toBeInstanceOf(ImageFilterCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await ImageFilterCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).toBeInstanceOf(ImageFilterCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ImageFilterCollection.fetchAll();
        for(let imageFilter of collection) {
          expect(imageFilter).toBeInstanceOf(ImageFilter);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ImageFilterCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ImageFilter());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ImageFilterCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ImageFilterCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ImageFilterCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ImageFilterCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });*/

});
