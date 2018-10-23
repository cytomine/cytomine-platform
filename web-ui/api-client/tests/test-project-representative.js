import * as utils from "./utils.js";
import {ProjectRepresentative, ProjectRepresentativeCollection} from "@";

describe("ProjectRepresentative", function() {

    let user;
    let projectInstance;
    let project;

    let projectRepresentative;
    let id = 0;

    before(async function() {
        await utils.connect(true);
        projectInstance = await utils.getProject();
        project = projectInstance.id;
        ({id: user} = await utils.getUser());
        await projectInstance.addUser(user);
    });

    after(async function() {
        await utils.cleanData();
    });

    describe("Create", function() {
        it("Create", async function() {
            projectRepresentative = new ProjectRepresentative({user, project});
            projectRepresentative = await projectRepresentative.save();
            expect(projectRepresentative).to.be.an.instanceof(ProjectRepresentative);
            id = projectRepresentative.id;
            expect(id).to.exist;
        });
    });

    describe("Fetch", function() {
        it("Fetch with static method", async function() {
            let fetchedProjectRepresentative = await ProjectRepresentative.fetch(id, project);
            expect(fetchedProjectRepresentative).to.be.an.instanceof(ProjectRepresentative);
            expect(fetchedProjectRepresentative).to.deep.equal(projectRepresentative);
        });

        it("Fetch with instance method", async function() {
            let fetchedProjectRepresentative = await new ProjectRepresentative({project, id}).fetch();
            expect(fetchedProjectRepresentative).to.be.an.instanceof(ProjectRepresentative);
            expect(fetchedProjectRepresentative).to.deep.equal(projectRepresentative);
        });

        it("Fetch with wrong ID", function() {
            expect(ProjectRepresentative.fetch(0)).to.be.rejected;
        });
    });

    describe("Delete", function() {
        it("Delete", async function() {
            await ProjectRepresentative.delete(id, project);
        });

        it("Fetch deleted", function() {
            expect(ProjectRepresentative.fetch(id, project)).to.be.rejected;
        });
    });

    // --------------------

    describe("ProjectRepresentativeCollection", function() {

        let nbProjectRepresentatives = 3;
        let projectRepresentatives;

        before(async function() {
            async function createUserAndProjectRepresentative() {
                let {id: tempUser} = await utils.getUser();
                await projectInstance.addUser(tempUser);
                let projectRepresentative = new ProjectRepresentative({project, user: tempUser});
                await projectRepresentative.save();
                return projectRepresentative;
            }

            let projectRepresentativePromises = [];
            for(let i = 0; i < nbProjectRepresentatives; i++) {
                projectRepresentativePromises.push(createUserAndProjectRepresentative());
            }
            projectRepresentatives = await Promise.all(projectRepresentativePromises);
        });

        after(async function() {
            let deletionPromises = projectRepresentatives.map(projectRepresentative =>
                ProjectRepresentative.delete(projectRepresentative.id, project));
            await Promise.all(deletionPromises);
        });

        describe("Fetch", function() {
            it("Fetch (instance method)", async function() {
                let collection = await new ProjectRepresentativeCollection({filterKey: "project", filterValue: project}).fetchAll();
                expect(collection).to.be.an.instanceof(ProjectRepresentativeCollection);
                expect(collection).to.have.lengthOf(nbProjectRepresentatives);
            });

            it("Fetch (static method)", async function() {
                let collection = await ProjectRepresentativeCollection.fetchAll({filterKey: "project", filterValue: project});
                expect(collection).to.be.an.instanceof(ProjectRepresentativeCollection);
                expect(collection).to.have.lengthOf(nbProjectRepresentatives);
            });

            it("Fetch with several requests", async function() {
                let collection = await ProjectRepresentativeCollection.fetchAll({nbPerPage: 1,
                    filterKey: "project", filterValue: project});
                expect(collection).to.be.an.instanceof(ProjectRepresentativeCollection);
                expect(collection).to.have.lengthOf(nbProjectRepresentatives);
            });

            it("Fetch without filter", async function() {
                let collection = new ProjectRepresentativeCollection();
                expect(collection.fetchAll()).to.be.rejected;
            });
        });

        describe("Working with the collection", function() {
            it("Iterate through", async function() {
                let collection = await ProjectRepresentativeCollection.fetchAll({filterKey: "project", filterValue: project});
                for(let projectRepresentative of collection) {
                    expect(projectRepresentative).to.be.an.instanceof(ProjectRepresentative);
                }
            });

            it("Add item to the collection", function() {
                let collection = new ProjectRepresentativeCollection();
                expect(collection).to.have.lengthOf(0);
                collection.push(new ProjectRepresentative());
                expect(collection).to.have.lengthOf(1);
            });

            it("Add arbitrary object to the collection", function() {
                let collection = new ProjectRepresentativeCollection();
                expect(collection.push.bind(collection, {})).to.throw();
            });
        });

        describe("Pagination", function() {
            let nbPerPage = 1;

            it("Fetch arbitrary page", async function() {
                let collection = new ProjectRepresentativeCollection({nbPerPage, filterKey: "project", filterValue: project});
                await collection.fetchPage(2);
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch next page", async function() {
                let collection = new ProjectRepresentativeCollection({nbPerPage, filterKey: "project", filterValue: project});
                await collection.fetchNextPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });

            it("Fetch previous page", async function() {
                let collection = new ProjectRepresentativeCollection({nbPerPage, filterKey: "project", filterValue: project});
                collection.curPage = 2;
                await collection.fetchPreviousPage();
                expect(collection).to.have.lengthOf(nbPerPage);
            });
        });

    });

});
