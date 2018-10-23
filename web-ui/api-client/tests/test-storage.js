import * as utils from "./utils.js";
import {Storage, StorageCollection} from "@";

describe("Storage", function() {

    let name = utils.randomString();
    let user;

    let storageUser = null;
    let storage = null;
    let id = 0;

    before(async function() {
        await utils.connect(true);
        ({id: user} = await utils.getUser());
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        // skipped because a storage for the user seems to be created automatically and this method throws error if the user already possesses a storage
        it.skip("Create for user", async function() {
            storageUser = await Storage.create(user);
            expect(storageUser).to.be.an.instanceof(Storage);
            expect(storageUser.user).to.equal(user);
        });

        it("Create", async function() {
            storage = new Storage({name, user, basePath: name});
            storage = await storage.save();
            id = storage.id;
            expect(storage).to.be.an.instanceof(Storage);
            expect(id).to.exist;
            expect(storage.name).to.equal(name);
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedStorage = await Storage.fetch(id);
            expect(fetchedStorage).to.be.an.instanceof(Storage);
            expect(fetchedStorage).to.deep.equal(storage);
        });

        it("Fetch with instance method", async function() {
            let fetchedStorage = await new Storage({id}).fetch();
            expect(fetchedStorage).to.be.an.instanceof(Storage);
            expect(fetchedStorage).to.deep.equal(storage);
        });

        it("Fetch with wrong ID", function() {
            expect(Storage.fetch(0)).to.be.rejected;
        });
    });

    describe("Update", function() {
        it("Update", async function() {
            let newName = utils.randomString();
            storage.name = newName;
            storage = await storage.update();
            expect(storage).to.be.an.instanceof(Storage);
            expect(storage.name).to.equal(newName);
        });
    });

    describe.skip("Delete", function() { // TODO: Remove skip once bug in core is fixed
        it("Delete", async function() {
            await Storage.delete(id);
        });

        it("Fetch deleted", function() {
            expect(Storage.fetch(id)).to.be.rejected;
        });
    });

    // --------------------

    describe("StorageCollection", function() {

        let nbStorages = 3;
        let totalNb = 0;

        before(async function() {
            let storagePromises = [];
            for(let i = 0; i < nbStorages - 1; i++) {
                let str = utils.randomString();
                storagePromises.push(new Storage({name: str, user, basePath: str}).save());
            }
            await Promise.all(storagePromises);
        });

        // remark: not required to clean manually the created storages ; the deletion of the user will lead to their deletions

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new StorageCollection().fetchAll();
                expect(collection).to.be.an.instanceof(StorageCollection);
                expect(collection).to.have.lengthOf.at.least(nbStorages);
                totalNb = collection.length;
            });

            it("Fetch (static method)", async function() {
                let collection = await StorageCollection.fetchAll();
                expect(collection).to.be.an.instanceof(StorageCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it("Fetch with several requests", async function() {
                let collection = await StorageCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
                expect(collection).to.be.an.instanceof(StorageCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await StorageCollection.fetchAll();
                for(let storage of collection) {
                    expect(storage).to.be.an.instanceof(Storage);
                }
            });

            it("Add item to the collection", function() {
                let collection = new StorageCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new Storage());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new StorageCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new StorageCollection({nbPerPage});
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new StorageCollection({nbPerPage});
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new StorageCollection({nbPerPage});
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
