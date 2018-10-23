import * as utils from "./utils.js";
import {ProjectDefaultLayer, ProjectDefaultLayerCollection} from "@";

describe("ProjectDefaultLayer", function() {

    let user;
    let project;

    let projectDefaultLayer;
    let id = 0;

    before(async function() {
        await utils.connect(true);
        ({id: project} = await utils.getProject());
        ({id: user} = await utils.getUser());
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            projectDefaultLayer = new ProjectDefaultLayer({user, project});
            projectDefaultLayer = await projectDefaultLayer.save();
            expect(projectDefaultLayer).to.be.an.instanceof(ProjectDefaultLayer);
            id = projectDefaultLayer.id;
            expect(id).to.exist;
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedProjectDefaultLayer = await ProjectDefaultLayer.fetch(id, project);
            expect(fetchedProjectDefaultLayer).to.be.an.instanceof(ProjectDefaultLayer);
            expect(fetchedProjectDefaultLayer).to.deep.equal(projectDefaultLayer);
        });

        it("Fetch with instance method", async function() {
            let fetchedProjectDefaultLayer = await new ProjectDefaultLayer({project, id}).fetch();
            expect(fetchedProjectDefaultLayer).to.be.an.instanceof(ProjectDefaultLayer);
            expect(fetchedProjectDefaultLayer).to.deep.equal(projectDefaultLayer);
        });

        it("Fetch with wrong ID", function() {
            expect(ProjectDefaultLayer.fetch(0)).to.be.rejected;
        });
    });

    describe("Update", function() {
        it("Update", async function() {
            let hideByDefault = !projectDefaultLayer.hideByDefault;
            projectDefaultLayer.hideByDefault = hideByDefault;
            projectDefaultLayer = await projectDefaultLayer.update();
            expect(projectDefaultLayer).to.be.an.instanceof(ProjectDefaultLayer);
            expect(projectDefaultLayer.hideByDefault).to.equal(hideByDefault);
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await ProjectDefaultLayer.delete(id, project);
        });

        it("Fetch deleted", function() {
            expect(ProjectDefaultLayer.fetch(id, project)).to.be.rejected;
        });
    });

    // --------------------

    describe("ProjectDefaultLayerCollection", function() {

        let nbProjectDefaultLayers = 3;
        let projectDefaultLayers;

        before(async function() {
            async function createUserAndProjectDefaultLayer() {
                let tempUser = await utils.getUser();
                let projectDefaultLayer = new ProjectDefaultLayer({project, user: tempUser.id});
                await projectDefaultLayer.save();
                return projectDefaultLayer;
            }

            let projectDefaultLayerPromises = [];
            for(let i = 0; i < nbProjectDefaultLayers; i++) {
                projectDefaultLayerPromises.push(createUserAndProjectDefaultLayer());
            }
            projectDefaultLayers = await Promise.all(projectDefaultLayerPromises);
        });

        after(async function() {
            let deletionPromises = projectDefaultLayers.map(projectDefaultLayer =>
                ProjectDefaultLayer.delete(projectDefaultLayer.id, project));
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new ProjectDefaultLayerCollection({filterKey: "project", filterValue: project}).fetchAll();
                expect(collection).to.be.an.instanceof(ProjectDefaultLayerCollection);
                expect(collection).to.have.lengthOf(nbProjectDefaultLayers);
            });

            it("Fetch (static method)", async function() {
                let collection = await ProjectDefaultLayerCollection.fetchAll({filterKey: "project", filterValue: project});
                expect(collection).to.be.an.instanceof(ProjectDefaultLayerCollection);
                expect(collection).to.have.lengthOf(nbProjectDefaultLayers);
            });

            it("Fetch with several requests", async function() {
                let collection = await ProjectDefaultLayerCollection.fetchAll({nbPerPage: 1, 
                    filterKey: "project", filterValue: project});
                expect(collection).to.be.an.instanceof(ProjectDefaultLayerCollection);
                expect(collection).to.have.lengthOf(nbProjectDefaultLayers);
            });

            it("Fetch without filter", async function() {
                let collection = new ProjectDefaultLayerCollection();
                expect(collection.fetchAll()).to.be.rejected;
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await ProjectDefaultLayerCollection.fetchAll({filterKey: "project", filterValue: project});
                for(let projectDefaultLayer of collection) {
                    expect(projectDefaultLayer).to.be.an.instanceof(ProjectDefaultLayer);
                }
            });

            it("Add item to the collection", function() {
                let collection = new ProjectDefaultLayerCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new ProjectDefaultLayer());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new ProjectDefaultLayerCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new ProjectDefaultLayerCollection({nbPerPage, filterKey: "project", filterValue: project});
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new ProjectDefaultLayerCollection({nbPerPage, filterKey: "project", filterValue: project});
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new ProjectDefaultLayerCollection({nbPerPage, filterKey: "project", filterValue: project});
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
