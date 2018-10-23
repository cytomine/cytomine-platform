import * as utils from "./utils.js";
import {Role, RoleCollection} from "@";

describe("Role", function() {
    let totalNb = 0;

    let role;

    before(async function() {
        await utils.connect(true);
    });

    describe("Fetch collection", function() {
        it("Fetch (instance method)", async function() {
            let collection = await new RoleCollection().fetchAll();
            expect(collection).to.be.an.instanceof(RoleCollection);
            totalNb = collection.length;
            role = collection.get(0);
        });

        it("Fetch (static method)", async function() {
            let collection = await RoleCollection.fetchAll();
            expect(collection).to.be.an.instanceof(RoleCollection);
            expect(collection).to.have.lengthOf(totalNb);
        });

        it("Fetch with several requests", async function() {
            let collection = await RoleCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
            expect(collection).to.be.an.instanceof(RoleCollection);
            expect(collection).to.have.lengthOf(totalNb);
        });
    });

    describe("Working with the collection", function() {
        it("Iterate through", async function() {
            let collection = await RoleCollection.fetchAll();
            for(let role of collection) {
                expect(role).to.be.an.instanceof(Role);
            }
        });

        it("Add item to the collection", function() {
            let collection = new RoleCollection();
            expect(collection).to.have.lengthOf(0);
            collection.push(new Role());
            expect(collection).to.have.lengthOf(1);
        });

        it("Add arbitrary object to the collection", function() {
            let collection = new RoleCollection();
            expect(collection.push.bind(collection, {})).to.throw();
        });
    });

    describe("Pagination", function() {
        let nbPerPage = 1;

        it("Fetch arbitrary page", async function() {
            let collection = new RoleCollection({nbPerPage});
            await collection.fetchPage(2);
            expect(collection).to.have.lengthOf(nbPerPage);
        });

        it("Fetch next page", async function() {
            let collection = new RoleCollection({nbPerPage});
            await collection.fetchNextPage();
            expect(collection).to.have.lengthOf(nbPerPage);
        });

        it("Fetch previous page", async function() {
            let collection = new RoleCollection({nbPerPage});
            collection.curPage = 2;
            await collection.fetchPreviousPage();
            expect(collection).to.have.lengthOf(nbPerPage);
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedRole = await Role.fetch(role.id);
            expect(fetchedRole).to.be.an.instanceof(Role);
            expect(fetchedRole).to.deep.equal(role);
        });

        it("Fetch with instance method", async function() {
            let fetchedRole = await new Role({id: role.id}).fetch();
            expect(fetchedRole).to.be.an.instanceof(Role);
            expect(fetchedRole).to.deep.equal(role);
        });

        it.skip("Fetch with wrong ID", function() { // Inconsistent behaviour: returns null instead of an error
            expect(Role.fetch(0)).to.be.rejected;
        });
    });

});
