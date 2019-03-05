import * as utils from "./utils.js";
import {ProjectConnection, ProjectConnectionCollection, User} from "@";

describe("ProjectConnection", function() {
    let project;
    let projectConnection = null;

    before(async function() {
        await utils.connect();
        ({id: project} = await utils.getProject());
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            projectConnection = new ProjectConnection({project});
            await projectConnection.save();
            expect(projectConnection).to.be.an.instanceof(ProjectConnection);
            expect(projectConnection.id).to.be.above(0);
        });
    });

    describe("Fetch", function() {
        it("Fetch", function() {
            expect(projectConnection.fetch.bind(projectConnection)).to.throw();
        });
    });

    describe("Update", function() {
        it("Update", function() {
            expect(projectConnection.update.bind(projectConnection)).to.throw();
        });
    });

    describe("Delete", function() {
        it("Delete", function() {
            expect(projectConnection.delete.bind(projectConnection)).to.throw();
        });
    });


    // --------------------

    describe("ProjectConnectionCollection", function() {

        let nbConnections = 3;
        let totalNb = 0;
        let user;

        before(async function() {
            ({id: user} = await User.fetchCurrent());

            let connectionsPromise = [];
            for(let i = 0; i < nbConnections; i++) {
                connectionsPromise.push(new ProjectConnection({project}).save());
            }
            connectionsPromise = await Promise.all(connectionsPromise);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new ProjectConnectionCollection({user, project}).fetchAll();
                expect(collection).to.be.an.instanceof(ProjectConnectionCollection);
                expect(collection).to.have.lengthOf.at.least(nbConnections);
                totalNb = collection.length;
            });

            it("Fetch (static method)", async function() {
                let collection = await ProjectConnectionCollection.fetchAll({user, project});
                expect(collection).to.be.an.instanceof(ProjectConnectionCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it.skip("Fetch with several requests", async function() { // incorrect values returned for size and totalPages
                let collection = await ProjectConnectionCollection.fetchAll({user, project, nbPerPage: Math.ceil(totalNb/3)});
                expect(collection).to.be.an.instanceof(ProjectConnectionCollection);
                expect(collection).to.have.lengthOf(totalNb);
            });

            it("Fetch without filter", async function() {
                let collection = new ProjectConnectionCollection();
                expect(collection.fetchAll()).to.be.rejected;
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await ProjectConnectionCollection.fetchAll({user, project});
                for(let connection of collection) {
                    expect(connection).to.be.an.instanceof(ProjectConnection);
                }
            });

            it("Add item to the collection", function() {
                let collection = new ProjectConnectionCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new ProjectConnection());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new ProjectConnectionCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });

            it("Download URL", async function() {
                let collection = new ProjectConnectionCollection({user, project});
                expect(collection.downloadURL).to.be.a("string");
            });
        });

        describe("Specific operations", function() {
            it("Fetch average connections", async function() {
                let result = await ProjectConnectionCollection.fetchAverageConnections({project, beforeThan: new Date().getTime()});
                expect(result).to.be.instanceof(Array);
            });

            it("Fetch connections frequency", async function() {
                let result = await ProjectConnectionCollection.fetchConnectionsFrequency({project, beforeThan: new Date().getTime()});
                expect(result).to.be.instanceof(Array);
            });
        });
        
        
        describe.skip("Pagination", function() { // incorrect values returned for size and totalPages
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new ProjectConnectionCollection({user, project, nbPerPage});
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new ProjectConnectionCollection({user, project, nbPerPage});
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new ProjectConnectionCollection({user, project, nbPerPage});
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
